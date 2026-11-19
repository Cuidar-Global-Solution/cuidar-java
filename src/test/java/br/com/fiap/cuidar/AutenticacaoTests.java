package br.com.fiap.cuidar;

import br.com.fiap.cuidar.dao.ContaDAO;
import br.com.fiap.cuidar.dao.PessoaAcompanhadaDAO;
import br.com.fiap.cuidar.dao.RegistroCuidadoDAO;
import br.com.fiap.cuidar.dao.SessaoDAO;
import br.com.fiap.cuidar.dao.VinculoCuidadoDAO;
import br.com.fiap.cuidar.models.Conta;
import br.com.fiap.cuidar.models.Conta.Perfil;
import br.com.fiap.cuidar.models.RegistroCuidado;
import br.com.fiap.cuidar.models.PessoaAcompanhada;
import br.com.fiap.cuidar.models.VinculoProfissional;
import br.com.fiap.cuidar.models.VinculoProfissional.Permissao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "CUIDAR_CORS_ALLOWED_ORIGINS=https://cuidar-front.vercel.app")
class AutenticacaoTests {
    private static final String ORIGEM = "https://cuidar-front.vercel.app";
    private static final String SENHA = "SenhaFicticia2026";
    private static final Conta FAMILIAR = new Conta(10L, "Maria Exemplo", "maria@example.test", Perfil.FAMILIAR);
    private static final String CADASTRO = """
            {"nome":" Maria Exemplo ","email":" MARIA@EXAMPLE.TEST ","senha":"SenhaFicticia2026","perfil":"FAMILIAR"}
            """;
    private static final String REGISTRO = """
            {"pessoaAcompanhadaId":4,"areaId":1,"titulo":"Registro fictício",
             "status":"PENDENTE","dataPrevista":"2026-11-18T09:00:00"}
            """;

    @Autowired private WebApplicationContext contexto;
    @Autowired private FilterChainProxy seguranca;
    @Autowired private ObjectMapper json;
    @Autowired private PasswordEncoder encoder;

    @MockitoBean private DataSource dataSource;
    @MockitoBean private ContaDAO contas;
    @MockitoBean private SessaoDAO sessoes;
    @MockitoBean private VinculoCuidadoDAO vinculos;
    @MockitoBean private PessoaAcompanhadaDAO pessoas;
    @MockitoBean private RegistroCuidadoDAO registros;

    private MockMvc mvc;

    @BeforeEach
    void configurarMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(seguranca).build();
    }

    @Test
    void daoBuscaSomenteVinculosAtivosDaContaComPermissaoCompativel() throws Exception {
        Connection conexao = mock(Connection.class);
        PreparedStatement leitura = mock(PreparedStatement.class);
        PreparedStatement gravacao = mock(PreparedStatement.class);
        ResultSet pessoasAtivas = mock(ResultSet.class);
        ResultSet semPermissao = mock(ResultSet.class);
        ResultSetMetaData metadados = mock(ResultSetMetaData.class);
        when(dataSource.getConnection()).thenReturn(conexao);
        when(conexao.prepareStatement(argThat(sql -> sql != null && sql.contains("FROM TB_VINCULO")
                && sql.contains("CD_USUARIO = ? AND ST_AUTORIZACAO = 'ATIVO'")
                && sql.contains("TP_PERMISSAO IN ('VISUALIZAR', 'REGISTRAR')")))).thenReturn(leitura);
        when(conexao.prepareStatement(argThat(sql -> sql != null && sql.contains("FROM TB_VINCULO")
                && sql.contains("CD_USUARIO = ? AND ST_AUTORIZACAO = 'ATIVO'")
                && sql.contains("CD_PESSOA_ACOMPANHADA = ? AND TP_PERMISSAO = 'REGISTRAR'")))).thenReturn(gravacao);
        when(leitura.executeQuery()).thenReturn(pessoasAtivas);
        when(pessoasAtivas.getMetaData()).thenReturn(metadados);
        when(metadados.getColumnCount()).thenReturn(1);
        when(pessoasAtivas.next()).thenReturn(true, true, false);
        when(pessoasAtivas.getLong(1)).thenReturn(3L, 4L);
        when(gravacao.executeQuery()).thenReturn(semPermissao);

        var dao = new VinculoCuidadoDAO(dataSource);
        // A consulta retorna os vínculos ativos. Pendentes e revogados ficam fora do resultado JDBC.
        assertEquals(Set.of(3L, 4L), dao.pessoasPermitidas(FAMILIAR));
        assertFalse(dao.podeRegistrar(FAMILIAR.id(), 4L));
        verify(leitura).setObject(1, FAMILIAR.id());
        verify(gravacao).setObject(1, FAMILIAR.id());
        verify(gravacao).setObject(2, 4L);
    }

    @Test
    void cadastroValidaNormalizaEGravaApenasHash() throws Exception {
        when(contas.cadastrar(eq("Maria Exemplo"), eq(FAMILIAR.email()), anyString(), eq(Perfil.FAMILIAR)))
                .thenAnswer(invocacao -> {
                    String hash = invocacao.getArgument(2);
                    assertNotEquals(SENHA, hash);
                    assertTrue(encoder.matches(SENHA, hash));
                    return FAMILIAR;
                });

        mvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.email").value(FAMILIAR.email())).andExpect(jsonPath("$.perfil").value("FAMILIAR"))
                .andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.hash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());

        for (String invalido : List.of(
                CADASTRO.replace("FAMILIAR", "ADMIN"),
                CADASTRO.replace("\"FAMILIAR\"", "0"),
                CADASTRO.replace("Maria Exemplo", ""),
                CADASTRO.replace("MARIA@EXAMPLE.TEST", "email-invalido"),
                CADASTRO.replace(SENHA, "curta"),
                CADASTRO.replace(SENHA, "é".repeat(37)))) {
            mvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(invalido))
                    .andExpect(status().isBadRequest());
        }
        verify(contas, times(1)).cadastrar(anyString(), anyString(), anyString(), any());
        verifyNoInteractions(sessoes);
    }

    @Test
    void emailDuplicadoRetorna409SemDadosDoOracle() throws Exception {
        var conexao = mock(Connection.class);
        var comando = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(conexao);
        when(conexao.prepareStatement(anyString(), any(String[].class))).thenReturn(comando);
        when(comando.executeUpdate()).thenThrow(new SQLException("mensagem interna do Oracle", "23000", 1));
        ContaDAO jdbc = new ContaDAO(dataSource);
        when(contas.cadastrar(anyString(), anyString(), anyString(), any()))
                .thenAnswer(i -> jdbc.cadastrar(i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3)));

        mvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensagem").value("O e-mail informado já está cadastrado."));
        verify(comando).close();
        verify(conexao).close();
    }

    @Test
    void senhaIncorretaNaoCriaSessao() throws Exception {
        when(contas.buscarCredencial(FAMILIAR.email())).thenReturn(
                Optional.of(new ContaDAO.Credencial(FAMILIAR, encoder.encode(SENHA))));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"email":"maria@example.test","senha":"senha-incorreta"}
                                 """))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sessoes);
    }

    @Test
    void loginSessaoAposNovaRequisicaoELogoutEntreOrigens() throws Exception {
        when(contas.buscarCredencial(FAMILIAR.email())).thenReturn(
                Optional.of(new ContaDAO.Credencial(FAMILIAR, encoder.encode(SENHA))));
        var hashAtivo = new AtomicReference<String>();
        doAnswer(i -> {
            hashAtivo.set(i.getArgument(0));
            return null;
        }).when(sessoes).criar(anyString(), eq(FAMILIAR.id()), eq(28800L));
        when(sessoes.buscarAtiva(anyString())).thenAnswer(i -> i.getArgument(0).equals(hashAtivo.get())
                ? Optional.of(FAMILIAR) : Optional.empty());
        doAnswer(i -> {
            hashAtivo.set(null);
            return null;
        }).when(sessoes).revogar(anyString());

        mvc.perform(options("/auth/sessao").header(HttpHeaders.ORIGIN, ORIGEM)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));

        var resposta = mvc.perform(post("/auth/login").header(HttpHeaders.ORIGIN, ORIGEM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"email":"maria@example.test","senha":"SenhaFicticia2026","perfil":"PROFISSIONAL"}
                                 """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conta.perfil").value("FAMILIAR"))
                .andExpect(jsonPath("$.expiraEmSegundos").value(28800))
                .andExpect(jsonPath("$.conta.senha").doesNotExist()).andExpect(jsonPath("$.conta.hash").doesNotExist())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE)).andReturn().getResponse();
        String token = json.readTree(resposta.getContentAsString()).get("token").stringValue();
        String bearer = "Bearer " + token;
        String digest = hash(token);
        assertEquals(digest, hashAtivo.get());

        mvc.perform(get("/auth/sessao").header(HttpHeaders.ORIGIN, ORIGEM).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("FAMILIAR"))
                .andExpect(jsonPath("$.token").doesNotExist());
        mvc.perform(get("/auth/sessao")).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/logout").header(HttpHeaders.ORIGIN, ORIGEM).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
        verify(sessoes).revogar(digest);
        mvc.perform(get("/auth/sessao").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void familiarVeSoPessoasERegistrosVinculadosENaoAlteraOutraFamilia() throws Exception {
        String token = "a".repeat(43);
        String bearer = "Bearer " + token;
        when(sessoes.buscarAtiva(hash(token))).thenReturn(Optional.of(FAMILIAR));
        when(vinculos.pessoasPermitidas(FAMILIAR)).thenReturn(Set.of(3L));
        Conta outroFamiliar = new Conta(12L, "Outro familiar", "outro@example.test", Perfil.FAMILIAR);
        String outroToken = "d".repeat(43);
        when(sessoes.buscarAtiva(hash(outroToken))).thenReturn(Optional.of(outroFamiliar));
        when(vinculos.pessoasPermitidas(outroFamiliar)).thenReturn(Set.of(4L));

        var pessoaPropria = new PessoaAcompanhada();
        pessoaPropria.setId(3L);
        var pessoaAlheia = new PessoaAcompanhada();
        pessoaAlheia.setId(4L);
        when(pessoas.listar()).thenReturn(List.of(pessoaPropria, pessoaAlheia));

        var proprio = new RegistroCuidado();
        proprio.setId(9L);
        proprio.setPessoaAcompanhadaId(3L);
        var alheio = new RegistroCuidado();
        alheio.setId(11L);
        alheio.setPessoaAcompanhadaId(4L);
        when(registros.listar()).thenReturn(List.of(proprio, alheio));
        when(registros.buscarPorId(11L)).thenReturn(Optional.of(alheio));

        mvc.perform(get("/pessoas-acompanhadas").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(3));
        mvc.perform(get("/registros-cuidado").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(9));
        mvc.perform(get("/pessoas-acompanhadas/4").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
        mvc.perform(get("/pessoas-acompanhadas/4/registros").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
        mvc.perform(get("/registros-cuidado/11").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
        mvc.perform(post("/registros-cuidado").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTRO))
                .andExpect(status().isNotFound());
        mvc.perform(put("/registros-cuidado/11").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTRO.replace(":4", ":3")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/registros-cuidado/11").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
        mvc.perform(get("/pessoas-acompanhadas/3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + outroToken))
                .andExpect(status().isNotFound());
        verify(pessoas).listar();
        verifyNoMoreInteractions(pessoas);
        verify(registros, never()).cadastrar(any());
        verify(registros, never()).atualizar(any());
        verify(registros, never()).excluir(anyLong());
    }

    @Test
    void visualizarPermiteLeituraMasSoRegistrarPermiteExcluirRegistro() throws Exception {
        String token = "e".repeat(43);
        String bearer = "Bearer " + token;
        when(sessoes.buscarAtiva(hash(token))).thenReturn(Optional.of(FAMILIAR));
        when(vinculos.pessoasPermitidas(FAMILIAR)).thenReturn(Set.of(4L));
        var registro = new RegistroCuidado();
        registro.setId(11L);
        registro.setPessoaAcompanhadaId(4L);
        when(registros.buscarPorId(11L)).thenReturn(Optional.of(registro));

        mvc.perform(get("/registros-cuidado/11").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());
        mvc.perform(post("/registros-cuidado").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTRO))
                .andExpect(status().isForbidden());
        mvc.perform(put("/registros-cuidado/11").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTRO))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/registros-cuidado/11").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isForbidden());
        verify(registros, never()).cadastrar(any());
        verify(registros, never()).atualizar(any());
        verify(registros, never()).excluir(anyLong());

        when(vinculos.podeRegistrar(FAMILIAR.id(), 4L)).thenReturn(true);
        when(registros.excluir(11L)).thenReturn(true);
        mvc.perform(delete("/registros-cuidado/11").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
        verify(registros).excluir(11L);
    }

    @Test
    void excluirPessoaComVinculoRetornaConflitoSemExcluirOVinculo() throws Exception {
        String token = "f".repeat(43);
        when(sessoes.buscarAtiva(hash(token))).thenReturn(Optional.of(FAMILIAR));
        when(vinculos.pessoasPermitidas(FAMILIAR)).thenReturn(Set.of(4L));
        when(vinculos.podeRegistrar(FAMILIAR.id(), 4L)).thenReturn(true);

        Connection conexao = mock(Connection.class);
        PreparedStatement comando = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(conexao);
        when(conexao.prepareStatement("DELETE FROM TB_PESSOA_ACOMPANHADA WHERE CD_PESSOA_ACOMPANHADA = ?"))
                .thenReturn(comando);
        when(comando.executeUpdate()).thenThrow(new SQLException(
                "ORA-02292: FK_TB_VINCULO_PESSOA possui registro filho", "23000", 2292));
        var dao = new PessoaAcompanhadaDAO(dataSource);
        when(pessoas.excluir(4L)).thenAnswer(chamada -> dao.excluir(chamada.getArgument(0)));

        mvc.perform(delete("/pessoas-acompanhadas/4").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(comando).setLong(1, 4L);
        verify(comando).executeUpdate();
        verify(conexao, times(1)).prepareStatement(anyString());
        verify(vinculos).pessoasPermitidas(FAMILIAR);
        verify(vinculos).podeRegistrar(FAMILIAR.id(), 4L);
        verifyNoMoreInteractions(vinculos);
    }

    @Test
    void contaAcompanhadaTemLeituraEContaFamiliarNaoPodeReivindicarVinculo() throws Exception {
        Conta acompanhado = new Conta(20L, "João Exemplo", "joao@example.test", Perfil.ACOMPANHADO);
        String tokenAcompanhado = "b".repeat(43);
        String tokenFamiliar = "c".repeat(43);
        when(sessoes.buscarAtiva(hash(tokenAcompanhado))).thenReturn(Optional.of(acompanhado));
        when(sessoes.buscarAtiva(hash(tokenFamiliar))).thenReturn(Optional.of(FAMILIAR));
        when(vinculos.pessoasPermitidas(acompanhado)).thenReturn(Set.of(3L));
        var pessoa = new PessoaAcompanhada();
        pessoa.setId(3L);
        when(pessoas.buscarPorId(3L)).thenReturn(Optional.of(pessoa));
        var registroProprio = new RegistroCuidado();
        registroProprio.setId(19L);
        registroProprio.setPessoaAcompanhadaId(3L);
        when(registros.listarPorPessoa(3L)).thenReturn(List.of(registroProprio));

        mvc.perform(get("/pessoas-acompanhadas/3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAcompanhado))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(3));
        mvc.perform(get("/pessoas-acompanhadas/3/registros")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAcompanhado))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(19));
        mvc.perform(get("/pessoas-acompanhadas/4/registros")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAcompanhado))
                .andExpect(status().isNotFound());
        mvc.perform(get("/pessoas-acompanhadas/3/permissoes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAcompanhado))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permissao").value("VISUALIZAR"));
        mvc.perform(post("/registros-cuidado").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAcompanhado)
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTRO.replace(":4", ":3")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/pessoas-acompanhadas").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFamiliar)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"usuarioId":20,"nome":"Pessoa fictícia","dataNascimento":"1950-01-02",
                                 "tamanhoFonte":18,"altoContraste":"N"}
                                """))
                .andExpect(status().isForbidden());

        verify(pessoas, never()).cadastrarComFamiliar(any(), anyLong());
        verify(registros).listarPorPessoa(3L);
        verifyNoMoreInteractions(registros);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void listaAreasHabilitadasDaPessoaAcessivel(boolean temArea) throws Exception {
        String token = "g".repeat(43);
        when(sessoes.buscarAtiva(hash(token))).thenReturn(Optional.of(FAMILIAR));
        when(vinculos.pessoasPermitidas(FAMILIAR)).thenReturn(Set.of(3L));
        when(pessoas.buscarPorId(3L)).thenReturn(Optional.of(new PessoaAcompanhada()));

        Connection conexao = mock(Connection.class);
        PreparedStatement consulta = mock(PreparedStatement.class);
        ResultSet resultado = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(conexao);
        when(conexao.prepareStatement(argThat(sql -> sql
                .replaceAll("\\s+", " ").contains("FROM TB_PESSOA_AREA_CUIDADO pa "
                        + "JOIN TB_AREA_CUIDADO a ON a.CD_AREA = pa.CD_AREA "
                        + "WHERE pa.CD_PESSOA_ACOMPANHADA = ?")))).thenReturn(consulta);
        when(consulta.executeQuery()).thenReturn(resultado);
        when(resultado.next()).thenReturn(temArea, false);

        if (temArea) {
            when(resultado.getLong("CD_AREA")).thenReturn(7L);
            when(resultado.getString("NM_AREA")).thenReturn("Hidratação");
            when(resultado.getString("DS_AREA")).thenReturn("Consumo de água");
        }

        var resposta = mvc.perform(get("/pessoas-acompanhadas/3/areas-cuidado")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        if (temArea) {
            resposta.andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(7))
                    .andExpect(jsonPath("$[0].nome").value("Hidratação"))
                    .andExpect(jsonPath("$[0].descricao").value("Consumo de água"));
        } else {
            resposta.andExpect(content().json("[]"));
        }
        verify(consulta).setLong(1, 3L);
    }

    @Test
    void areasHabilitadasRetornam404ParaOutraFamiliaOuPessoaInexistente() throws Exception {
        String token = "h".repeat(43);
        when(sessoes.buscarAtiva(hash(token))).thenReturn(Optional.of(FAMILIAR));
        when(vinculos.pessoasPermitidas(FAMILIAR)).thenReturn(Set.of(3L));
        when(pessoas.buscarPorId(3L)).thenReturn(Optional.empty());

        mvc.perform(get("/pessoas-acompanhadas/4/areas-cuidado")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(get("/pessoas-acompanhadas/3/areas-cuidado")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
        verify(pessoas).buscarPorId(3L);
        verifyNoMoreInteractions(pessoas);
        verifyNoInteractions(dataSource);
    }

    @Test
    void familiarHabilitaAreaDaPessoaRecemCadastradaECriaPrimeiroRegistro() throws Exception {
        String token = "i".repeat(43);
        String bearer = "Bearer " + token;
        String rota = "/pessoas-acompanhadas/3";
        String registro = """
                {"areaId":7,"titulo":"Beber água","status":"PENDENTE","dataPrevista":"2026-11-19T09:00:00"}
                """;
        when(sessoes.buscarAtiva(hash(token))).thenReturn(Optional.of(FAMILIAR));
        when(vinculos.pessoasPermitidas(FAMILIAR)).thenReturn(Set.of(3L));
        when(vinculos.podeRegistrar(FAMILIAR.id(), 3L)).thenReturn(true);
        when(vinculos.referenciasDaPessoa(3L, null, null)).thenReturn(true);
        var pessoaSalva = new AtomicReference<PessoaAcompanhada>();
        when(pessoas.cadastrarComFamiliar(any(), eq(FAMILIAR.id()))).thenAnswer(chamada -> {
            PessoaAcompanhada pessoa = chamada.getArgument(0);
            pessoa.setId(3L);
            pessoaSalva.set(pessoa);
            return pessoa;
        });
        when(pessoas.buscarPorId(3L)).thenAnswer(chamada -> Optional.ofNullable(pessoaSalva.get()));

        Connection conexao = mock(Connection.class);
        PreparedStatement catalogo = mock(PreparedStatement.class);
        PreparedStatement habilitar = mock(PreparedStatement.class);
        PreparedStatement listar = mock(PreparedStatement.class);
        ResultSet area = mock(ResultSet.class);
        ResultSet lista = mock(ResultSet.class);
        var areaHabilitada = new java.util.concurrent.atomic.AtomicBoolean();
        when(dataSource.getConnection()).thenReturn(conexao);
        when(conexao.prepareStatement("SELECT CD_AREA, NM_AREA, DS_AREA FROM TB_AREA_CUIDADO WHERE CD_AREA = ?"))
                .thenReturn(catalogo);
        when(conexao.prepareStatement(argThat(sql -> sql != null && sql.contains("INSERT INTO TB_PESSOA_AREA_CUIDADO")
                && sql.contains("SQ_TB_PESSOA_AREA_CUIDADO.NEXTVAL")))).thenReturn(habilitar);
        when(conexao.prepareStatement(contains("FROM TB_PESSOA_AREA_CUIDADO pa"))).thenReturn(listar);
        when(catalogo.executeQuery()).thenReturn(area);
        when(area.next()).thenReturn(true);
        when(habilitar.executeUpdate()).thenAnswer(chamada -> {
            if (areaHabilitada.getAndSet(true)) throw new SQLException("Par duplicado", "23000", 1);
            return 1;
        });
        when(listar.executeQuery()).thenReturn(lista);
        when(lista.next()).thenAnswer(chamada -> areaHabilitada.get()).thenReturn(false);
        when(lista.getLong("CD_AREA")).thenReturn(7L);
        when(lista.getString("NM_AREA")).thenReturn("Hidratação");
        when(registros.areaHabilitada(3L, 7L)).thenAnswer(chamada -> areaHabilitada.get());
        when(registros.cadastrar(any())).thenAnswer(chamada -> {
            RegistroCuidado salvo = chamada.getArgument(0);
            salvo.setId(20L);
            return salvo;
        });
        var existente = new RegistroCuidado();
        existente.setId(20L);
        existente.setPessoaAcompanhadaId(3L);
        existente.setUsuarioCriouId(FAMILIAR.id());
        when(registros.buscarPorId(20L)).thenReturn(Optional.of(existente));
        when(registros.atualizar(any())).thenReturn(true);
        when(registros.excluir(20L)).thenReturn(true);

        mvc.perform(post("/pessoas-acompanhadas").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nome":"Pessoa fictícia","dataNascimento":"1950-01-02",
                                 "tamanhoFonte":18,"altoContraste":"N"}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(3));
        mvc.perform(post(rota + "/registros").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isConflict());
        mvc.perform(post(rota + "/areas-cuidado").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":7}"))
                .andExpect(status().isNoContent());
        mvc.perform(get(rota + "/areas-cuidado").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(7));
        mvc.perform(post(rota + "/registros").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pessoaAcompanhadaId").value(3))
                .andExpect(jsonPath("$.areaId").value(7));
        mvc.perform(put("/registros-cuidado/20").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"pessoaAcompanhadaId":3,"areaId":7,"titulo":"Água servida",
                                 "status":"CONCLUIDO","dataRealizado":"2026-11-19T09:00:00"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuarioCriouId").value(FAMILIAR.id()));
        mvc.perform(delete("/registros-cuidado/20").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
        mvc.perform(post(rota + "/areas-cuidado").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":7}"))
                .andExpect(status().isConflict());
        verify(habilitar, times(2)).setLong(1, 3L);
        verify(habilitar, times(2)).setLong(2, 7L);
        verify(registros).cadastrar(any());
    }

    @Test
    void habilitarAreaBloqueiaOutraFamiliaEVinculoSomenteLeitura() throws Exception {
        String token = "j".repeat(43);
        String bearer = "Bearer " + token;
        when(sessoes.buscarAtiva(hash(token))).thenReturn(Optional.of(FAMILIAR));
        when(vinculos.pessoasPermitidas(FAMILIAR)).thenReturn(Set.of(3L));
        when(vinculos.podeRegistrar(FAMILIAR.id(), 3L)).thenReturn(false);
        mvc.perform(post("/pessoas-acompanhadas/4/areas-cuidado").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":7}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/pessoas-acompanhadas/3/areas-cuidado").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":7}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(pessoas, dataSource);
    }

    @ParameterizedTest
    @EnumSource(Perfil.class)
    void contaNovaCompletaCadastroLoginSessaoELogout(Perfil perfil) throws Exception {
        var conta = new Conta(70L, "Conta de teste", "nova@example.test", perfil);
        var credencial = new AtomicReference<ContaDAO.Credencial>();
        var sessao = new AtomicReference<String>();
        when(contas.cadastrar(eq(conta.nome()), eq(conta.email()), anyString(), eq(perfil))).thenAnswer(chamada -> {
            credencial.set(new ContaDAO.Credencial(conta, chamada.getArgument(2)));
            return conta;
        });
        when(contas.buscarCredencial(conta.email())).thenAnswer(chamada -> Optional.ofNullable(credencial.get()));
        doAnswer(chamada -> { sessao.set(chamada.getArgument(0)); return null; })
                .when(sessoes).criar(anyString(), eq(conta.id()), anyLong());
        when(sessoes.buscarAtiva(anyString())).thenAnswer(chamada ->
                chamada.getArgument(0).equals(sessao.get()) ? Optional.of(conta) : Optional.empty());
        doAnswer(chamada -> { sessao.set(null); return null; }).when(sessoes).revogar(anyString());

        mvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Conta de teste","email":"nova@example.test","senha":"SenhaFicticia2026","perfil":"%s"}
                """.formatted(perfil.name())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.perfil").value(perfil.name()));
        var login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"nova@example.test","senha":"SenhaFicticia2026"}
                """)).andExpect(status().isOk()).andReturn();
        String bearer = "Bearer " + json.readTree(login.getResponse().getContentAsString()).get("token").stringValue();
        mvc.perform(get("/auth/sessao").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value(perfil.name()));
        mvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isNoContent());
        mvc.perform(get("/auth/sessao").header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isUnauthorized());
    }

    @Test
    void conviteProfissionalRespeitaAceitePermissaoRevogacaoEIsolamento() throws Exception {
        Conta profissional = new Conta(30L, "Profissional", "pro@example.test", Perfil.PROFISSIONAL);
        Conta outro = new Conta(31L, "Outro profissional", "outro@example.test", Perfil.PROFISSIONAL);
        String familiarBearer = autenticarContaSimulada(FAMILIAR, 'k');
        String profissionalBearer = autenticarContaSimulada(profissional, 'l');
        String outroBearer = autenticarContaSimulada(outro, 'm');
        String rotaPessoa = "/pessoas-acompanhadas/4";
        String rotaVinculos = rotaPessoa + "/vinculos-profissionais";
        String meus = "/vinculos-profissionais/meus";
        String convite = "{\"email\":\"PRO@EXAMPLE.TEST\",\"permissao\":\"VISUALIZAR\"}";
        var vinculo = new AtomicReference<VinculoProfissional>();
        when(vinculos.pessoasPermitidas(FAMILIAR)).thenReturn(Set.of(4L));
        when(vinculos.podeRegistrar(FAMILIAR.id(), 4L)).thenReturn(true);
        when(contas.buscarIdProfissional(profissional.email())).thenReturn(Optional.of(profissional.id()));
        when(vinculos.convidarProfissional(4L, profissional.id(), Permissao.VISUALIZAR)).thenAnswer(chamada -> {
            vinculo.set(new VinculoProfissional(8, 4, profissional.id(), profissional.nome(), Permissao.VISUALIZAR, "PENDENTE"));
            return true;
        });
        when(vinculos.listarDoProfissional(profissional.id())).thenAnswer(chamada -> List.of(vinculo.get()));
        when(vinculos.aceitarConvite(profissional.id(), 8L)).thenAnswer(chamada -> {
            vinculo.set(new VinculoProfissional(8, 4, profissional.id(), profissional.nome(), Permissao.VISUALIZAR, "ATIVO"));
            return true;
        });
        when(vinculos.alterarPermissao(4L, 8L, Permissao.REGISTRAR)).thenAnswer(chamada -> {
            vinculo.set(new VinculoProfissional(8, 4, profissional.id(), profissional.nome(), Permissao.REGISTRAR, "ATIVO"));
            return true;
        });
        when(vinculos.revogarProfissional(4L, 8L)).thenAnswer(chamada -> {
            vinculo.set(new VinculoProfissional(8, 4, profissional.id(), profissional.nome(), Permissao.REGISTRAR, "REVOGADO"));
            return true;
        });
        when(vinculos.pessoasPermitidas(profissional)).thenAnswer(chamada ->
                "ATIVO".equals(vinculo.get().autorizacao()) ? Set.of(4L) : Set.of());
        when(vinculos.podeRegistrar(profissional.id(), 4L)).thenAnswer(chamada ->
                "ATIVO".equals(vinculo.get().autorizacao()) && vinculo.get().permissao() == Permissao.REGISTRAR);
        when(pessoas.buscarPorId(4L)).thenReturn(Optional.of(new PessoaAcompanhada()));
        var registro = new RegistroCuidado();
        registro.setId(11L);
        registro.setPessoaAcompanhadaId(4L);
        when(registros.buscarPorId(11L)).thenReturn(Optional.of(registro));
        when(registros.excluir(11L)).thenReturn(true);

        mvc.perform(post("/pessoas-acompanhadas/5/vinculos-profissionais").header(HttpHeaders.AUTHORIZATION, familiarBearer)
                        .contentType(MediaType.APPLICATION_JSON).content(convite)).andExpect(status().isNotFound());
        mvc.perform(post(rotaVinculos).header(HttpHeaders.AUTHORIZATION, familiarBearer)
                        .contentType(MediaType.APPLICATION_JSON).content(convite)).andExpect(status().isNoContent());
        mvc.perform(get(meus).header(HttpHeaders.AUTHORIZATION, profissionalBearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].autorizacao").value("PENDENTE"));
        mvc.perform(get(rotaPessoa + "/registros").header(HttpHeaders.AUTHORIZATION, profissionalBearer))
                .andExpect(status().isNotFound());
        mvc.perform(post(meus + "/8/aceitar").header(HttpHeaders.AUTHORIZATION, outroBearer))
                .andExpect(status().isNotFound());
        mvc.perform(post(meus + "/8/aceitar").header(HttpHeaders.AUTHORIZATION, profissionalBearer))
                .andExpect(status().isNoContent());
        mvc.perform(get(rotaPessoa + "/registros").header(HttpHeaders.AUTHORIZATION, profissionalBearer))
                .andExpect(status().isOk());
        mvc.perform(get(rotaPessoa + "/permissoes").header(HttpHeaders.AUTHORIZATION, profissionalBearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permissao").value("VISUALIZAR"));
        mvc.perform(delete("/registros-cuidado/11").header(HttpHeaders.AUTHORIZATION, profissionalBearer))
                .andExpect(status().isForbidden());
        mvc.perform(put(rotaVinculos + "/8/permissao").header(HttpHeaders.AUTHORIZATION, familiarBearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"permissao\":\"REGISTRAR\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get(rotaPessoa + "/permissoes").header(HttpHeaders.AUTHORIZATION, profissionalBearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permissao").value("REGISTRAR"));
        mvc.perform(delete("/registros-cuidado/11").header(HttpHeaders.AUTHORIZATION, profissionalBearer))
                .andExpect(status().isNoContent());
        mvc.perform(delete(rotaVinculos + "/8").header(HttpHeaders.AUTHORIZATION, familiarBearer))
                .andExpect(status().isNoContent());
        mvc.perform(get(rotaPessoa + "/registros").header(HttpHeaders.AUTHORIZATION, profissionalBearer))
                .andExpect(status().isNotFound());
        verify(registros).excluir(11L);
        verify(vinculos).convidarProfissional(4L, profissional.id(), Permissao.VISUALIZAR);
    }

    private String autenticarContaSimulada(Conta conta, char caractere) throws Exception {
        String token = String.valueOf(caractere).repeat(43);
        when(sessoes.buscarAtiva(hash(token))).thenReturn(Optional.of(conta));
        return "Bearer " + token;
    }

    private static String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.US_ASCII)));
    }
}

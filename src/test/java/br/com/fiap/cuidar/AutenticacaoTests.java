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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        verify(pessoas).listar();
        verifyNoMoreInteractions(pessoas);
        verify(registros, never()).cadastrar(any());
        verify(registros, never()).atualizar(any());
        verify(registros, never()).excluir(anyLong());
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

        mvc.perform(get("/pessoas-acompanhadas/3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAcompanhado))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(3));
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
        verifyNoInteractions(registros);
    }

    private static String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.US_ASCII)));
    }
}

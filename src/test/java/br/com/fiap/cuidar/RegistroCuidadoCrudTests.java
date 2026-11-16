package br.com.fiap.cuidar;

import br.com.fiap.cuidar.controller.*;
import br.com.fiap.cuidar.dao.RegistroCuidadoDAO;
import br.com.fiap.cuidar.exception.*;
import br.com.fiap.cuidar.handler.GlobalExceptionHandler;
import br.com.fiap.cuidar.mapper.RegistroCuidadoMapper;
import br.com.fiap.cuidar.models.*;
import br.com.fiap.cuidar.service.*;
import org.junit.jupiter.api.*;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegistroCuidadoCrudTests {
    private static final String ROTA = "/registros-cuidado";
    private static final String ANINHADA = "/pessoas-acompanhadas/3/registros";
    private static final String JSON = """
            {"pessoaAcompanhadaId":3,"areaId":1,"usuarioCriouId":2,
             "titulo":"Beber água","status":"PENDENTE","dataPrevista":"2026-11-20T10:30:00",
             "medida":250.50,"unidade":"ml"}
            """;
    private RegistroCuidadoDAO dao;
    private PessoaAcompanhadaService pessoas;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void configurar() {
        dao = mock(RegistroCuidadoDAO.class);
        pessoas = mock(PessoaAcompanhadaService.class);
        var service = new RegistroCuidadoService(dao, pessoas);
        var mapper = new RegistroCuidadoMapper(new ModelMapper());
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new RegistroCuidadoController(service, mapper),
                new PessoaRegistroCuidadoController(service, mapper))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @AfterEach
    void fechar() { validator.close(); }

    @Test
    void cadastrarRetorna201LocationEMapeamentoSemConfundirIds() throws Exception {
        when(dao.cadastrar(any())).thenAnswer(i -> {
            RegistroCuidado r = i.getArgument(0);
            assertNull(r.getId());
            assertEquals(3L, r.getPessoaAcompanhadaId());
            assertEquals(1L, r.getAreaId());
            assertEquals(2L, r.getUsuarioCriouId());
            assertNull(r.getUsuarioRealizouId());
            r.setId(9L);
            return r;
        });
        mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/registros-cuidado/9"))
                .andExpect(jsonPath("$.id").value(9)).andExpect(jsonPath("$.pessoaAcompanhadaId").value(3))
                .andExpect(jsonPath("$.titulo").value("Beber água"))
                .andExpect(jsonPath("$.medida").value(250.50));
    }

    @Test
    void listarBuscarAtualizarExcluir() throws Exception {
        var r = new RegistroCuidado(); r.setId(9L); r.setTitulo("Beber água");
        when(dao.listar()).thenReturn(List.of(r));
        when(dao.buscarPorId(9L)).thenReturn(Optional.of(r));
        when(dao.atualizar(any())).thenReturn(true);
        when(dao.excluir(9L)).thenReturn(true);
        mvc.perform(get(ROTA)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(9));
        mvc.perform(get(ROTA + "/9")).andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Beber água"));
        mvc.perform(put(ROTA + "/9").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.pessoaAcompanhadaId").value(3));
        mvc.perform(delete(ROTA + "/9")).andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    void inexistentesRetornam404Json() throws Exception {
        when(dao.buscarPorId(99L)).thenReturn(Optional.empty());
        erro(mvc.perform(get(ROTA + "/99")), 404, ROTA + "/99");
        erro(mvc.perform(put(ROTA + "/99").contentType(MediaType.APPLICATION_JSON).content(JSON)), 404, ROTA + "/99");
        erro(mvc.perform(delete(ROTA + "/99")), 404, ROTA + "/99");
    }

    @Test
    void validacoesV3Retornam400SemChamarDao() throws Exception {
        for (String corpo : List.of("{}", JSON.replace("Beber água", " "),
                JSON.replace("Beber água", "A".repeat(101)), JSON.replace("PENDENTE", "INVALIDO"),
                JSON.replace("\"areaId\":1", "\"areaId\":1000"),
                JSON.replace("250.50", "1000000.00"), JSON.replace("250.50", "1.123"),
                JSON.replace("\"dataPrevista\":\"2026-11-20T10:30:00\"", "\"dataPrevista\":null"),
                JSON.replace("\"unidade\":\"ml\"", "\"unidade\":\"" + "A".repeat(21) + "\""))) {
            erro(mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(corpo)), 400, ROTA);
            erro(mvc.perform(put(ROTA + "/9").contentType(MediaType.APPLICATION_JSON).content(corpo)), 400, ROTA + "/9");
        }
        erro(mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content("{")), 400, ROTA);
        erro(mvc.perform(get(ROTA + "/0")), 400, ROTA + "/0");
        verifyNoInteractions(dao);
    }

    @Test
    void aceitaSomenteDataRealizada() throws Exception {
        when(dao.cadastrar(any())).thenAnswer(i -> { RegistroCuidado r = i.getArgument(0); r.setId(9L); return r; });
        mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(JSON.replace("dataPrevista", "dataRealizado")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.dataPrevista").isEmpty())
                .andExpect(jsonPath("$.dataRealizado").value("2026-11-20T10:30:00"));
    }

    @Test
    void conflitoRetorna409EMetodoNaoPermitido405() throws Exception {
        when(dao.cadastrar(any())).thenThrow(new ConflitoException("Uma referência informada não existe.", new java.sql.SQLException("SQL privado")));
        erro(mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(JSON)), 409, ROTA);
        erro(mvc.perform(patch(ROTA + "/9")), 405, ROTA + "/9").andExpect(header().exists("Allow"));
    }

    @Test
    void rotasAninhadasVerificamPessoaEUsamIdDaRota() throws Exception {
        when(pessoas.buscarPorId(3L)).thenReturn(new PessoaAcompanhada());
        when(dao.listarPorPessoa(3L)).thenReturn(List.of());
        mvc.perform(get(ANINHADA)).andExpect(status().isOk()).andExpect(content().json("[]"));
        when(dao.cadastrar(any())).thenAnswer(i -> {
            RegistroCuidado r = i.getArgument(0); assertEquals(3L, r.getPessoaAcompanhadaId());
            r.setId(9L); return r;
        });
        mvc.perform(post(ANINHADA).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.replace("\"pessoaAcompanhadaId\":3,", "")))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/registros-cuidado/9"))
                .andExpect(jsonPath("$.pessoaAcompanhadaId").value(3));
        verify(pessoas, times(2)).buscarPorId(3L);
        verify(dao).listarPorPessoa(3L);
    }

    @Test
    void pessoaInexistenteNasRotasAninhadasRetorna404() throws Exception {
        when(pessoas.buscarPorId(3L)).thenThrow(new PessoaAcompanhadaNaoEncontradaException());
        erro(mvc.perform(get(ANINHADA)), 404, ANINHADA);
        erro(mvc.perform(post(ANINHADA).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.replace("\"pessoaAcompanhadaId\":3,", ""))), 404, ANINHADA);
        verifyNoInteractions(dao);
    }

    private ResultActions erro(ResultActions resultado, int codigo, String caminho) throws Exception {
        return resultado.andExpect(status().is(codigo)).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isString()).andExpect(jsonPath("$.status").value(codigo))
                .andExpect(jsonPath("$.erro").isString()).andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.caminho").value(caminho)).andExpect(jsonPath("$.detalhes").isArray())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL privado"))));
    }
}

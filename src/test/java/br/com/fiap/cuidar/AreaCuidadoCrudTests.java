package br.com.fiap.cuidar;

import br.com.fiap.cuidar.controller.AreaCuidadoController;
import br.com.fiap.cuidar.dao.AreaCuidadoDAO;
import br.com.fiap.cuidar.exception.ConflitoException;
import br.com.fiap.cuidar.handler.GlobalExceptionHandler;
import br.com.fiap.cuidar.mapper.AreaCuidadoMapper;
import br.com.fiap.cuidar.models.AreaCuidado;
import br.com.fiap.cuidar.service.AreaCuidadoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AreaCuidadoCrudTests {
    private static final String ROTA = "/areas-cuidado";
    private static final String JSON = """
            {"nome":"Alimentação","descricao":"Apoio nas refeições"}
            """;
    private AreaCuidadoDAO dao;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void configurar() {
        dao = mock(AreaCuidadoDAO.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new AreaCuidadoController(
                new AreaCuidadoService(dao), new AreaCuidadoMapper(new ModelMapper())))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @AfterEach
    void fecharValidador() { validator.close(); }

    @Test
    void cadastrarRetorna201LocationEDadosMapeados() throws Exception {
        when(dao.cadastrar(any())).thenAnswer(invocation -> {
            AreaCuidado area = invocation.getArgument(0);
            assertNull(area.getId());
            assertEquals("Alimentação", area.getNome());
            assertEquals("Apoio nas refeições", area.getDescricao());
            area.setId(7L);
            return area;
        });
        mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/areas-cuidado/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nome").value("Alimentação"))
                .andExpect(jsonPath("$.descricao").value("Apoio nas refeições"));
    }

    @Test
    void aceitaNomeNoLimiteEDescricaoOpcionalOuNoLimite() throws Exception {
        when(dao.cadastrar(any())).thenAnswer(invocation -> {
            AreaCuidado area = invocation.getArgument(0);
            area.setId(7L);
            return area;
        });
        String nome = "A".repeat(50);
        mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + nome + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.descricao").isEmpty());
        String descricao = "D".repeat(200);
        mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + nome + "\",\"descricao\":\"" + descricao + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.descricao").value(descricao));
    }

    @Test
    void listarEBuscarRetornam200ComDados() throws Exception {
        AreaCuidado area = new AreaCuidado();
        area.setId(7L);
        area.setNome("Alimentação");
        area.setDescricao("Apoio nas refeições");
        when(dao.listar()).thenReturn(List.of(area));
        when(dao.buscarPorId(7L)).thenReturn(Optional.of(area));
        mvc.perform(get(ROTA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].nome").value("Alimentação"))
                .andExpect(jsonPath("$[0].descricao").value("Apoio nas refeições"));
        mvc.perform(get(ROTA + "/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nome").value("Alimentação"))
                .andExpect(jsonPath("$.descricao").value("Apoio nas refeições"));
    }

    @Test
    void listaVaziaRetorna200ComArrayVazio() throws Exception {
        when(dao.listar()).thenReturn(List.of());
        mvc.perform(get(ROTA)).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void editarRetorna200ComIdDaRotaEDadosAtualizados() throws Exception {
        when(dao.atualizar(any())).thenAnswer(invocation -> {
            AreaCuidado area = invocation.getArgument(0);
            assertEquals(7L, area.getId());
            assertEquals("Alimentação", area.getNome());
            assertEquals("Apoio nas refeições", area.getDescricao());
            return true;
        });
        mvc.perform(put(ROTA + "/7").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nome").value("Alimentação"))
                .andExpect(jsonPath("$.descricao").value("Apoio nas refeições"));
        verify(dao).atualizar(any());
    }

    @Test
    void excluirRetorna204SemCorpo() throws Exception {
        when(dao.excluir(7L)).thenReturn(true);
        mvc.perform(delete(ROTA + "/7")).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(dao).excluir(7L);
    }

    @Test
    void entradaInvalidaRetorna400JsonNoCadastroENaEdicao() throws Exception {
        for (String corpo : List.of("{}", "{\"nome\":null}", "{\"nome\":\" \"}",
                JSON.replace("Alimentação", "A".repeat(51)),
                JSON.replace("Apoio nas refeições", "D".repeat(201)))) {
            conferirErro(mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(corpo)),
                    400, "Bad Request", ROTA).andExpect(jsonPath("$.detalhes").isNotEmpty());
            conferirErro(mvc.perform(put(ROTA + "/7").contentType(MediaType.APPLICATION_JSON).content(corpo)),
                    400, "Bad Request", ROTA + "/7");
        }
        verifyNoInteractions(dao);
    }

    @Test
    void jsonMalformadoEIdInvalidoRetornam400Json() throws Exception {
        conferirErro(mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content("{")),
                400, "Bad Request", ROTA);
        for (String id : List.of("abc", "0", "-1")) {
            conferirErro(mvc.perform(get(ROTA + "/" + id)), 400, "Bad Request", ROTA + "/" + id);
        }
        verifyNoInteractions(dao);
    }

    @Test
    void idInexistenteRetorna404JsonNaBuscaEdicaoEExclusao() throws Exception {
        when(dao.buscarPorId(99L)).thenReturn(Optional.empty());
        when(dao.atualizar(any())).thenReturn(false);
        when(dao.excluir(99L)).thenReturn(false);
        for (ResultActions resultado : List.of(mvc.perform(get(ROTA + "/99")),
                mvc.perform(put(ROTA + "/99").contentType(MediaType.APPLICATION_JSON).content(JSON)),
                mvc.perform(delete(ROTA + "/99")))) {
            conferirErro(resultado, 404, "Not Found", ROTA + "/99")
                    .andExpect(jsonPath("$.mensagem").value("Área de cuidado não encontrada."))
                    .andExpect(jsonPath("$.detalhes").isEmpty());
        }
    }

    @Test
    void nomeDuplicadoRetorna409JsonNoCadastroENaEdicaoSemExporSql() throws Exception {
        var conflito = new ConflitoException("Já existe uma área de cuidado com esse nome.",
                new SQLException("SQL privado", "23000", 1));
        when(dao.cadastrar(any())).thenThrow(conflito);
        when(dao.atualizar(any())).thenThrow(conflito);
        conferirErro(mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(JSON)),
                409, "Conflict", ROTA)
                .andExpect(jsonPath("$.mensagem").value(conflito.getMessage()))
                .andExpect(jsonPath("$.detalhes").isEmpty());
        conferirErro(mvc.perform(put(ROTA + "/7").contentType(MediaType.APPLICATION_JSON).content(JSON)),
                409, "Conflict", ROTA + "/7")
                .andExpect(jsonPath("$.mensagem").value(conflito.getMessage()))
                .andExpect(jsonPath("$.detalhes").isEmpty());
    }

    private ResultActions conferirErro(ResultActions resultado, int status, String erro, String caminho)
            throws Exception {
        return resultado.andExpect(status().is(status))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.erro").value(erro))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.caminho").value(caminho))
                .andExpect(jsonPath("$.detalhes").isArray())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.cause").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL privado"))));
    }
}

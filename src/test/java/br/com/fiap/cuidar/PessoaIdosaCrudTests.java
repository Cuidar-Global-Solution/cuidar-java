package br.com.fiap.cuidar;

import br.com.fiap.cuidar.controller.PessoaIdosaController;
import br.com.fiap.cuidar.dao.PessoaIdosaDAO;
import br.com.fiap.cuidar.exception.ConflitoException;
import br.com.fiap.cuidar.exception.PersistenciaException;
import br.com.fiap.cuidar.handler.GlobalExceptionHandler;
import br.com.fiap.cuidar.mapper.PessoaIdosaMapper;
import br.com.fiap.cuidar.models.PessoaIdosa;
import br.com.fiap.cuidar.service.PessoaIdosaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PessoaIdosaCrudTests {
    private PessoaIdosaDAO dao;
    private MockMvc mvc;
    private static final String JSON = """
            {"usuarioId":null,"nome":"Maria","dataNascimento":"1950-01-02",
             "observacao":null,"tamanhoFonte":18,"altoContraste":"S"}
            """;

    @BeforeEach
    void configurar() {
        dao = mock(PessoaIdosaDAO.class);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new PessoaIdosaController(
                new PessoaIdosaService(dao), new PessoaIdosaMapper(new ModelMapper())))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @Test
    void cadastrarRetornaIdGeradoLocationECamposOpcionais() throws Exception {
        when(dao.cadastrar(any())).thenAnswer(invocation -> {
            PessoaIdosa pessoa = invocation.getArgument(0);
            org.junit.jupiter.api.Assertions.assertNull(pessoa.getId());
            org.junit.jupiter.api.Assertions.assertNull(pessoa.getUsuarioId());
            pessoa.setId(42L);
            return pessoa;
        });
        mvc.perform(post("/idosos").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/idosos/42"))
                .andExpect(jsonPath("$.id").value(42)).andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.usuarioId").isEmpty()).andExpect(jsonPath("$.observacao").isEmpty());
    }

    @Test
    void vinculoOpcionalNaoSeConfundeComIdDaPessoa() throws Exception {
        when(dao.cadastrar(any())).thenAnswer(invocation -> {
            PessoaIdosa pessoa = invocation.getArgument(0);
            org.junit.jupiter.api.Assertions.assertNull(pessoa.getId());
            org.junit.jupiter.api.Assertions.assertEquals(7L, pessoa.getUsuarioId());
            pessoa.setId(42L);
            return pessoa;
        });
        mvc.perform(post("/idosos").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.replace("\"usuarioId\":null", "\"usuarioId\":7")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.usuarioId").value(7));
    }

    @Test
    void listarBuscarAtualizarEExcluir() throws Exception {
        var pessoa = new PessoaIdosa();
        pessoa.setId(42L);
        when(dao.listar()).thenReturn(List.of(pessoa));
        when(dao.buscarPorId(42L)).thenReturn(Optional.of(pessoa));
        when(dao.atualizar(any())).thenReturn(true);
        when(dao.excluir(42L)).thenReturn(true);
        mvc.perform(get("/idosos")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(42));
        mvc.perform(get("/idosos/42")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(42));
        mvc.perform(put("/idosos/42").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.nome").value("Maria"));
        mvc.perform(delete("/idosos/42")).andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    void inexistentesRetornam404() throws Exception {
        when(dao.buscarPorId(99L)).thenReturn(Optional.empty());
        mvc.perform(get("/idosos/99")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(put("/idosos/99").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/idosos/99")).andExpect(status().isNotFound());
    }

    @Test
    void entradaInvalidaJsonMalformadoEIdInvalidoRetornam400() throws Exception {
        mvc.perform(post("/idosos").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.replace("Maria", " ").replace("18", "33").replace("\"S\"", "\"X\"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalhes.length()").value(3));
        mvc.perform(post("/idosos").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/idosos/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/idosos/0")).andExpect(status().isBadRequest());
        verifyNoInteractions(dao);
    }

    @Test
    void metodoNaoPermitidoRetorna405JsonEAllow() throws Exception {
        mvc.perform(patch("/idosos/42").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.status").value(405))
                .andExpect(header().exists("Allow")).andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void conflitoEFalhaInternaNaoExpoemSql() throws Exception {
        when(dao.excluir(42L)).thenThrow(new ConflitoException(
                "A pessoa idosa possui registros relacionados e não pode ser removida.",
                new SQLException("DELETE FROM TB_PESSOA_IDOSA")));
        mvc.perform(delete("/idosos/42")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.trace").doesNotExist());
        when(dao.listar()).thenThrow(new PersistenciaException(new SQLException("SELECT segredo")));
        mvc.perform(get("/idosos")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensagem").value("Erro interno ao processar a requisição."))
                .andExpect(jsonPath("$.detalhes").isEmpty());
    }
}

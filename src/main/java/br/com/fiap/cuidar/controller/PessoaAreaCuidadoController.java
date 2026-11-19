package br.com.fiap.cuidar.controller;

import br.com.fiap.cuidar.dto.AreaCuidadoResponse;
import br.com.fiap.cuidar.dto.HabilitarAreaCuidadoRequest;
import br.com.fiap.cuidar.mapper.AreaCuidadoMapper;
import br.com.fiap.cuidar.service.AcessoCuidado;
import br.com.fiap.cuidar.service.AreaCuidadoService;
import br.com.fiap.cuidar.service.PessoaAcompanhadaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pessoas-acompanhadas/{id}/areas-cuidado")
public class PessoaAreaCuidadoController {
    private final AcessoCuidado acesso;
    private final PessoaAcompanhadaService pessoas;
    private final AreaCuidadoService areas;
    private final AreaCuidadoMapper mapper;

    public PessoaAreaCuidadoController(AcessoCuidado acesso, PessoaAcompanhadaService pessoas,
                                      AreaCuidadoService areas, AreaCuidadoMapper mapper) {
        this.acesso = acesso;
        this.pessoas = pessoas;
        this.areas = areas;
        this.mapper = mapper;
    }

    @GetMapping
    public List<AreaCuidadoResponse> listar(@PathVariable @Positive Long id) {
        acesso.lerPessoa(id);
        pessoas.buscarPorId(id);
        return areas.listarPorPessoa(id).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void habilitar(@PathVariable @Positive Long id, @Valid @RequestBody HabilitarAreaCuidadoRequest request) {
        acesso.alterarPessoa(id);
        pessoas.buscarPorId(id);
        areas.habilitarParaPessoa(id, request.areaId());
    }
}

package br.com.fiap.cuidar.controller;

import br.com.fiap.cuidar.dto.RegistroCuidadoAninhadoRequest;
import br.com.fiap.cuidar.dto.RegistroCuidadoResponse;
import br.com.fiap.cuidar.mapper.RegistroCuidadoMapper;
import br.com.fiap.cuidar.service.RegistroCuidadoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.List;

@RestController
@RequestMapping("/pessoas-acompanhadas/{id}/registros")
public class PessoaRegistroCuidadoController {
    private final RegistroCuidadoService service;
    private final RegistroCuidadoMapper mapper;

    public PessoaRegistroCuidadoController(RegistroCuidadoService service, RegistroCuidadoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<RegistroCuidadoResponse>> listar(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(service.listarPorPessoa(id).stream().map(mapper::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<RegistroCuidadoResponse> cadastrar(@PathVariable @Positive Long id,
            @Valid @RequestBody RegistroCuidadoAninhadoRequest request) {
        var registro = service.cadastrarPorPessoa(id, mapper.toEntity(request));
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/registros-cuidado/{id}").buildAndExpand(registro.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(registro));
    }
}

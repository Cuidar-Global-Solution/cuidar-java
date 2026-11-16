package br.com.fiap.cuidar.controller;

import br.com.fiap.cuidar.dto.PessoaAcompanhadaRequest;
import br.com.fiap.cuidar.dto.PessoaAcompanhadaResponse;
import br.com.fiap.cuidar.mapper.PessoaAcompanhadaMapper;
import br.com.fiap.cuidar.service.PessoaAcompanhadaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/pessoas-acompanhadas")
public class PessoaAcompanhadaController {
    private final PessoaAcompanhadaService service;
    private final PessoaAcompanhadaMapper mapper;

    public PessoaAcompanhadaController(PessoaAcompanhadaService service, PessoaAcompanhadaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<PessoaAcompanhadaResponse> cadastrar(@Valid @RequestBody PessoaAcompanhadaRequest request) {
        var pessoa = service.cadastrar(mapper.toEntity(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(pessoa.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(pessoa));
    }

    @GetMapping
    public ResponseEntity<List<PessoaAcompanhadaResponse>> listar() {
        return ResponseEntity.ok(service.listar().stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PessoaAcompanhadaResponse> buscarPorId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(mapper.toResponse(service.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PessoaAcompanhadaResponse> atualizar(@PathVariable @Positive Long id,
                                                        @Valid @RequestBody PessoaAcompanhadaRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.atualizar(id, mapper.toEntity(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable @Positive Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}

package br.com.fiap.cuidar.controller;

import br.com.fiap.cuidar.dto.PessoaIdosaRequest;
import br.com.fiap.cuidar.dto.PessoaIdosaResponse;
import br.com.fiap.cuidar.mapper.PessoaIdosaMapper;
import br.com.fiap.cuidar.service.PessoaIdosaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/idosos")
public class PessoaIdosaController {
    private final PessoaIdosaService service;
    private final PessoaIdosaMapper mapper;

    public PessoaIdosaController(PessoaIdosaService service, PessoaIdosaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<PessoaIdosaResponse> cadastrar(@Valid @RequestBody PessoaIdosaRequest request) {
        var pessoa = service.cadastrar(mapper.toEntity(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(pessoa.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(pessoa));
    }

    @GetMapping
    public ResponseEntity<List<PessoaIdosaResponse>> listar() {
        return ResponseEntity.ok(service.listar().stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PessoaIdosaResponse> buscarPorId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(mapper.toResponse(service.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PessoaIdosaResponse> atualizar(@PathVariable @Positive Long id,
                                                        @Valid @RequestBody PessoaIdosaRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.atualizar(id, mapper.toEntity(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable @Positive Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}

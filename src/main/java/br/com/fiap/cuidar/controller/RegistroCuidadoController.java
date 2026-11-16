package br.com.fiap.cuidar.controller;

import br.com.fiap.cuidar.dto.RegistroCuidadoRequest;
import br.com.fiap.cuidar.dto.RegistroCuidadoResponse;
import br.com.fiap.cuidar.mapper.RegistroCuidadoMapper;
import br.com.fiap.cuidar.service.RegistroCuidadoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/registros-cuidado")
public class RegistroCuidadoController {
    private final RegistroCuidadoService service;
    private final RegistroCuidadoMapper mapper;

    public RegistroCuidadoController(RegistroCuidadoService service, RegistroCuidadoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<RegistroCuidadoResponse> cadastrar(@Valid @RequestBody RegistroCuidadoRequest request) {
        var area = service.cadastrar(mapper.toEntity(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(area.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(area));
    }

    @GetMapping
    public ResponseEntity<List<RegistroCuidadoResponse>> listar() {
        return ResponseEntity.ok(service.listar().stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegistroCuidadoResponse> buscarPorId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(mapper.toResponse(service.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RegistroCuidadoResponse> atualizar(@PathVariable @Positive Long id,
                                                       @Valid @RequestBody RegistroCuidadoRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.atualizar(id, mapper.toEntity(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable @Positive Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}

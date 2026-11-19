package br.com.fiap.cuidar.controller;

import br.com.fiap.cuidar.dto.AreaCuidadoRequest;
import br.com.fiap.cuidar.dto.AreaCuidadoResponse;
import br.com.fiap.cuidar.mapper.AreaCuidadoMapper;
import br.com.fiap.cuidar.service.AreaCuidadoService;
import br.com.fiap.cuidar.service.AcessoCuidado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/areas-cuidado")
public class AreaCuidadoController {
    private final AreaCuidadoService service;
    private final AreaCuidadoMapper mapper;
    private final AcessoCuidado acesso;

    public AreaCuidadoController(AreaCuidadoService service, AreaCuidadoMapper mapper, AcessoCuidado acesso) {
        this.service = service;
        this.mapper = mapper;
        this.acesso = acesso;
    }

    @PostMapping
    public ResponseEntity<AreaCuidadoResponse> cadastrar(@Valid @RequestBody AreaCuidadoRequest request) {
        acesso.gerirAreas();
        var area = service.cadastrar(mapper.toEntity(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(area.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(area));
    }

    @GetMapping
    public ResponseEntity<List<AreaCuidadoResponse>> listar() {
        return ResponseEntity.ok(service.listar().stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AreaCuidadoResponse> buscarPorId(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(mapper.toResponse(service.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AreaCuidadoResponse> atualizar(@PathVariable @Positive Long id,
                                                       @Valid @RequestBody AreaCuidadoRequest request) {
        acesso.gerirAreas();
        return ResponseEntity.ok(mapper.toResponse(service.atualizar(id, mapper.toEntity(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable @Positive Long id) {
        acesso.gerirAreas();
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}

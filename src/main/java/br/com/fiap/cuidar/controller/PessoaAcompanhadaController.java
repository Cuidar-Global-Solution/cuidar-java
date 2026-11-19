package br.com.fiap.cuidar.controller;

import br.com.fiap.cuidar.dto.PessoaAcompanhadaRequest;
import br.com.fiap.cuidar.dto.PessoaAcompanhadaResponse;
import br.com.fiap.cuidar.mapper.PessoaAcompanhadaMapper;
import br.com.fiap.cuidar.service.PessoaAcompanhadaService;
import br.com.fiap.cuidar.service.AcessoCuidado;
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
    private final AcessoCuidado acesso;

    public PessoaAcompanhadaController(PessoaAcompanhadaService service, PessoaAcompanhadaMapper mapper, AcessoCuidado acesso) {
        this.service = service;
        this.mapper = mapper;
        this.acesso = acesso;
    }

    @PostMapping
    public ResponseEntity<PessoaAcompanhadaResponse> cadastrar(@Valid @RequestBody PessoaAcompanhadaRequest request) {
        var novaPessoa = mapper.toEntity(request);
        var pessoa = service.cadastrar(novaPessoa, acesso.autorizarNovaPessoa(novaPessoa));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(pessoa.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(pessoa));
    }

    @GetMapping
    public ResponseEntity<List<PessoaAcompanhadaResponse>> listar() {
        var permitidas = acesso.pessoasPermitidas();
        return ResponseEntity.ok(service.listar().stream()
                .filter(pessoa -> permitidas.contains(pessoa.getId())).map(mapper::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PessoaAcompanhadaResponse> buscarPorId(@PathVariable @Positive Long id) {
        acesso.lerPessoa(id);
        return ResponseEntity.ok(mapper.toResponse(service.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PessoaAcompanhadaResponse> atualizar(@PathVariable @Positive Long id,
                                                        @Valid @RequestBody PessoaAcompanhadaRequest request) {
        acesso.alterarPessoa(id);
        var pessoa = mapper.toEntity(request);
        acesso.validarContaDaPessoa(service.buscarPorId(id), pessoa);
        return ResponseEntity.ok(mapper.toResponse(service.atualizar(id, pessoa)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable @Positive Long id) {
        acesso.alterarPessoa(id);
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}

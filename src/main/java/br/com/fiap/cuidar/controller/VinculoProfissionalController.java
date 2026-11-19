package br.com.fiap.cuidar.controller;

import br.com.fiap.cuidar.models.Conta;
import br.com.fiap.cuidar.models.VinculoProfissional;
import br.com.fiap.cuidar.models.VinculoProfissional.Permissao;
import br.com.fiap.cuidar.service.VinculoProfissionalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class VinculoProfissionalController {
    private static final String PESSOA = "/pessoas-acompanhadas/{id}/vinculos-profissionais";
    private static final String MEUS = "/vinculos-profissionais/meus";
    private final VinculoProfissionalService service;

    public VinculoProfissionalController(VinculoProfissionalService service) {
        this.service = service;
    }

    @GetMapping(PESSOA)
    public List<VinculoProfissional> listar(@PathVariable @Positive long id) {
        return service.listarDaPessoa(id);
    }

    @PostMapping(PESSOA)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void convidar(@PathVariable @Positive long id, @Valid @RequestBody Convite request) {
        service.convidar(id, request.email(), request.permissao());
    }

    @PutMapping(PESSOA + "/{vinculoId}/permissao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarPermissao(@PathVariable @Positive long id, @PathVariable @Positive long vinculoId,
                                 @Valid @RequestBody Alteracao request) {
        service.alterarPermissao(id, vinculoId, request.permissao());
    }

    @DeleteMapping(PESSOA + "/{vinculoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revogar(@PathVariable @Positive long id, @PathVariable @Positive long vinculoId) {
        service.revogar(id, vinculoId);
    }

    @GetMapping(MEUS)
    public List<VinculoProfissional> listarMeus(@AuthenticationPrincipal Conta conta) {
        return service.listarDoProfissional(conta);
    }

    @PostMapping(MEUS + "/{vinculoId}/aceitar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aceitar(@AuthenticationPrincipal Conta conta, @PathVariable @Positive long vinculoId) {
        service.aceitar(conta, vinculoId);
    }

    public record Convite(@NotBlank @Email @Size(max = 120) String email, @NotNull Permissao permissao) {
        public Convite {
            if (email != null) email = email.strip();
        }
    }

    public record Alteracao(@NotNull Permissao permissao) {}
}

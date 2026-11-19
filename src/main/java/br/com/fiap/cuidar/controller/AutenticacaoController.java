package br.com.fiap.cuidar.controller;

import br.com.fiap.cuidar.dto.CadastroContaRequest;
import br.com.fiap.cuidar.dto.LoginRequest;
import br.com.fiap.cuidar.models.Conta;
import br.com.fiap.cuidar.service.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {
    private final AutenticacaoService autenticacao;

    public AutenticacaoController(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
    }

    @PostMapping(path = "/cadastro", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Conta cadastrar(@Valid @RequestBody CadastroContaRequest request) {
        return autenticacao.cadastrar(request);
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AutenticacaoService.Sessao login(@Valid @RequestBody LoginRequest request) {
        return autenticacao.login(request);
    }

    @GetMapping("/sessao")
    public Conta sessao(@AuthenticationPrincipal Conta conta) {
        return conta;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String bearer) {
        autenticacao.logout(bearer.substring(7));
    }
}

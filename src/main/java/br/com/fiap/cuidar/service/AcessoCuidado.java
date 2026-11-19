package br.com.fiap.cuidar.service;

import br.com.fiap.cuidar.dao.VinculoCuidadoDAO;
import br.com.fiap.cuidar.exception.PessoaAcompanhadaNaoEncontradaException;
import br.com.fiap.cuidar.exception.RegistroCuidadoNaoEncontradoException;
import br.com.fiap.cuidar.models.Conta;
import br.com.fiap.cuidar.models.Conta.Perfil;
import br.com.fiap.cuidar.models.PessoaAcompanhada;
import br.com.fiap.cuidar.models.RegistroCuidado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AcessoCuidado {
    private final VinculoCuidadoDAO vinculos;
    private final Set<Long> gestoresAreas;

    public AcessoCuidado(VinculoCuidadoDAO vinculos, @Value("${CUIDAR_AREA_MANAGER_IDS:}") String gestores) {
        this.vinculos = vinculos;
        this.gestoresAreas = Arrays.stream(gestores.split(","))
                .map(String::strip).filter(valor -> !valor.isEmpty())
                .map(Long::valueOf).collect(Collectors.toUnmodifiableSet());
    }

    public Set<Long> pessoasPermitidas() {
        return vinculos.pessoasPermitidas(contaAtual());
    }

    public long autorizarNovaPessoa(PessoaAcompanhada pessoa) {
        Conta conta = exigirFamiliar();
        if (pessoa.getUsuarioId() != null) {
            throw proibido("O vínculo com a conta da pessoa exige provisionamento.");
        }
        return conta.id();
    }

    public void lerPessoa(long id) {
        if (!pessoasPermitidas().contains(id)) throw new PessoaAcompanhadaNaoEncontradaException();
    }

    public void alterarPessoa(long id) {
        Conta conta = exigirFamiliar();
        lerPessoa(id);
        exigirVinculoDeGravacao(conta, id);
    }

    public boolean podeGravarRegistros(long pessoaId) {
        Conta conta = contaAtual();
        lerPessoa(pessoaId);
        return conta.perfil() != Perfil.ACOMPANHADO && vinculos.podeRegistrar(conta.id(), pessoaId);
    }

    public void validarContaDaPessoa(PessoaAcompanhada atual, PessoaAcompanhada alterada) {
        if (!Objects.equals(atual.getUsuarioId(), alterada.getUsuarioId())) {
            throw proibido("O vínculo com a conta da pessoa exige provisionamento.");
        }
    }

    public void lerRegistro(RegistroCuidado registro) {
        if (!pessoasPermitidas().contains(registro.getPessoaAcompanhadaId())) {
            throw new RegistroCuidadoNaoEncontradoException();
        }
    }

    public void validarGravacao(RegistroCuidado atual, RegistroCuidado alterado) {
        Conta conta = exigirEquipe();
        if (atual == null) {
            lerPessoa(alterado.getPessoaAcompanhadaId());
            exigirVinculoDeGravacao(conta, alterado.getPessoaAcompanhadaId());
            if (alterado.getUsuarioCriouId() != null && alterado.getUsuarioCriouId() != conta.id()) {
                throw proibido("O criador deve ser o usuário autenticado.");
            }
            alterado.setUsuarioCriouId(conta.id());
        } else {
            lerRegistro(atual);
            exigirVinculoDeGravacao(conta, atual.getPessoaAcompanhadaId());
            if (!Objects.equals(atual.getPessoaAcompanhadaId(), alterado.getPessoaAcompanhadaId())) {
                throw proibido("O registro deve permanecer com a pessoa acompanhada original.");
            }
            if (alterado.getUsuarioCriouId() != null
                    && !Objects.equals(atual.getUsuarioCriouId(), alterado.getUsuarioCriouId())) {
                throw proibido("O usuário criador é definido no cadastro.");
            }
            alterado.setUsuarioCriouId(atual.getUsuarioCriouId());
        }
        Long executor = alterado.getUsuarioRealizouId();
        if (executor != null && executor != conta.id()
                && (atual == null || !executor.equals(atual.getUsuarioRealizouId()))) {
            throw proibido("O executor deve ser o usuário autenticado ou o executor já registrado.");
        }
        if (!vinculos.referenciasDaPessoa(alterado.getPessoaAcompanhadaId(),
                alterado.getMedicamentoId(), alterado.getConsultaId())) {
            throw proibido("As referências devem pertencer à pessoa acompanhada.");
        }
    }

    public void excluirRegistro(RegistroCuidado registro) {
        Conta conta = exigirEquipe();
        lerRegistro(registro);
        exigirVinculoDeGravacao(conta, registro.getPessoaAcompanhadaId());
    }

    private void exigirVinculoDeGravacao(Conta conta, long pessoaId) {
        if (!vinculos.podeRegistrar(conta.id(), pessoaId)) {
            throw proibido("O vínculo ativo precisa permitir a gravação.");
        }
    }

    public void gerirAreas() {
        if (!gestoresAreas.contains(contaAtual().id())) {
            throw proibido("A conta não tem permissão para alterar o catálogo de áreas.");
        }
    }

    private Conta exigirFamiliar() {
        Conta conta = contaAtual();
        if (conta.perfil() != Perfil.FAMILIAR) throw proibido("Operação reservada a familiares vinculados.");
        return conta;
    }

    private Conta exigirEquipe() {
        Conta conta = contaAtual();
        if (conta.perfil() == Perfil.ACOMPANHADO) throw proibido("O perfil acompanhado tem acesso de leitura.");
        return conta;
    }

    private Conta contaAtual() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof Conta conta)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return conta;
    }

    private static ResponseStatusException proibido(String motivo) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, motivo);
    }
}

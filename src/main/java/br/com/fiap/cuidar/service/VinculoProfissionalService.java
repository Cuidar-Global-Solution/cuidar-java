package br.com.fiap.cuidar.service;

import br.com.fiap.cuidar.dao.ContaDAO;
import br.com.fiap.cuidar.dao.VinculoCuidadoDAO;
import br.com.fiap.cuidar.models.Conta;
import br.com.fiap.cuidar.models.VinculoProfissional;
import br.com.fiap.cuidar.models.VinculoProfissional.Permissao;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class VinculoProfissionalService {
    private final AcessoCuidado acesso;
    private final ContaDAO contas;
    private final VinculoCuidadoDAO vinculos;

    public VinculoProfissionalService(AcessoCuidado acesso, ContaDAO contas, VinculoCuidadoDAO vinculos) {
        this.acesso = acesso;
        this.contas = contas;
        this.vinculos = vinculos;
    }

    public List<VinculoProfissional> listarDaPessoa(long pessoaId) {
        acesso.alterarPessoa(pessoaId);
        return vinculos.listarProfissionais(pessoaId);
    }

    public void convidar(long pessoaId, String email, Permissao permissao) {
        acesso.alterarPessoa(pessoaId);
        long profissionalId = contas.buscarIdProfissional(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta profissional não encontrada."));
        if (!vinculos.convidarProfissional(pessoaId, profissionalId, permissao)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um vínculo ativo ou pendente.");
        }
    }

    public void alterarPermissao(long pessoaId, long vinculoId, Permissao permissao) {
        acesso.alterarPessoa(pessoaId);
        exigirVinculo(vinculos.alterarPermissao(pessoaId, vinculoId, permissao));
    }

    public void revogar(long pessoaId, long vinculoId) {
        acesso.alterarPessoa(pessoaId);
        exigirVinculo(vinculos.revogarProfissional(pessoaId, vinculoId));
    }

    public List<VinculoProfissional> listarDoProfissional(Conta conta) {
        return vinculos.listarDoProfissional(idProfissional(conta));
    }

    public void aceitar(Conta conta, long vinculoId) {
        exigirVinculo(vinculos.aceitarConvite(idProfissional(conta), vinculoId));
    }

    private long idProfissional(Conta conta) {
        if (conta.perfil() != Conta.Perfil.PROFISSIONAL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operação reservada a profissionais.");
        }
        return conta.id();
    }

    private void exigirVinculo(boolean alterado) {
        if (!alterado) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado para esta operação.");
        }
    }
}

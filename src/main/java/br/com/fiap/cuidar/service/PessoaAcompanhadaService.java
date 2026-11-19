package br.com.fiap.cuidar.service;

import br.com.fiap.cuidar.dao.PessoaAcompanhadaDAO;
import br.com.fiap.cuidar.exception.PessoaAcompanhadaNaoEncontradaException;
import br.com.fiap.cuidar.models.PessoaAcompanhada;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PessoaAcompanhadaService {
    private final PessoaAcompanhadaDAO dao;

    public PessoaAcompanhadaService(PessoaAcompanhadaDAO dao) { this.dao = dao; }

    public PessoaAcompanhada cadastrar(PessoaAcompanhada pessoa, long familiarId) {
        return dao.cadastrarComFamiliar(pessoa, familiarId);
    }
    public List<PessoaAcompanhada> listar() { return dao.listar(); }
    public PessoaAcompanhada buscarPorId(Long id) {
        return dao.buscarPorId(id).orElseThrow(PessoaAcompanhadaNaoEncontradaException::new);
    }
    public PessoaAcompanhada atualizar(Long id, PessoaAcompanhada pessoa) {
        pessoa.setId(id);
        if (!dao.atualizar(pessoa)) { throw new PessoaAcompanhadaNaoEncontradaException(); }
        return pessoa;
    }
    public void excluir(Long id) {
        if (!dao.excluir(id)) { throw new PessoaAcompanhadaNaoEncontradaException(); }
    }
}

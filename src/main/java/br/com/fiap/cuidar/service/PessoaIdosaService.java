package br.com.fiap.cuidar.service;

import br.com.fiap.cuidar.dao.PessoaIdosaDAO;
import br.com.fiap.cuidar.exception.PessoaIdosaNaoEncontradaException;
import br.com.fiap.cuidar.models.PessoaIdosa;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PessoaIdosaService {
    private final PessoaIdosaDAO dao;

    public PessoaIdosaService(PessoaIdosaDAO dao) { this.dao = dao; }

    public PessoaIdosa cadastrar(PessoaIdosa pessoa) { return dao.cadastrar(pessoa); }
    public List<PessoaIdosa> listar() { return dao.listar(); }
    public PessoaIdosa buscarPorId(Long id) {
        return dao.buscarPorId(id).orElseThrow(PessoaIdosaNaoEncontradaException::new);
    }
    public PessoaIdosa atualizar(Long id, PessoaIdosa pessoa) {
        pessoa.setId(id);
        if (!dao.atualizar(pessoa)) { throw new PessoaIdosaNaoEncontradaException(); }
        return pessoa;
    }
    public void excluir(Long id) {
        if (!dao.excluir(id)) { throw new PessoaIdosaNaoEncontradaException(); }
    }
}

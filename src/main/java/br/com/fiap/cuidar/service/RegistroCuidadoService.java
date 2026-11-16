package br.com.fiap.cuidar.service;

import br.com.fiap.cuidar.dao.RegistroCuidadoDAO;
import br.com.fiap.cuidar.exception.RegistroCuidadoNaoEncontradoException;
import br.com.fiap.cuidar.models.RegistroCuidado;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RegistroCuidadoService {
    private final RegistroCuidadoDAO dao;
    private final PessoaAcompanhadaService pessoas;

    public RegistroCuidadoService(RegistroCuidadoDAO dao, PessoaAcompanhadaService pessoas) { this.dao = dao; this.pessoas = pessoas; }
    public List<RegistroCuidado> listarPorPessoa(Long id) { pessoas.buscarPorId(id); return dao.listarPorPessoa(id); }
    public RegistroCuidado cadastrarPorPessoa(Long id, RegistroCuidado registro) { pessoas.buscarPorId(id); registro.setPessoaAcompanhadaId(id); return dao.cadastrar(registro); }

    public RegistroCuidado cadastrar(RegistroCuidado area) { return dao.cadastrar(area); }
    public List<RegistroCuidado> listar() { return dao.listar(); }
    public RegistroCuidado buscarPorId(Long id) {
        return dao.buscarPorId(id).orElseThrow(RegistroCuidadoNaoEncontradoException::new);
    }
    public RegistroCuidado atualizar(Long id, RegistroCuidado area) {
        area.setId(id);
        if (!dao.atualizar(area)) { throw new RegistroCuidadoNaoEncontradoException(); }
        return area;
    }
    public void excluir(Long id) {
        if (!dao.excluir(id)) { throw new RegistroCuidadoNaoEncontradoException(); }
    }
}

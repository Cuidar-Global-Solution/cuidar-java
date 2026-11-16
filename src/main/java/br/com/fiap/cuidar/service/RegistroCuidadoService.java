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
    private final AreaCuidadoService areas;

    public RegistroCuidadoService(RegistroCuidadoDAO dao, PessoaAcompanhadaService pessoas, AreaCuidadoService areas) {
        this.dao = dao;
        this.pessoas = pessoas;
        this.areas = areas;
    }
    public List<RegistroCuidado> listarPorPessoa(Long id) { pessoas.buscarPorId(id); return dao.listarPorPessoa(id); }
    public RegistroCuidado cadastrarPorPessoa(Long id, RegistroCuidado registro) {
        registro.setPessoaAcompanhadaId(id);
        return cadastrar(registro);
    }

    public RegistroCuidado cadastrar(RegistroCuidado registro) {
        validarAreaHabilitada(registro);
        return dao.cadastrar(registro);
    }

    private void validarAreaHabilitada(RegistroCuidado registro) {
        pessoas.buscarPorId(registro.getPessoaAcompanhadaId());
        areas.buscarPorId(registro.getAreaId());
        if (!dao.areaHabilitada(registro.getPessoaAcompanhadaId(), registro.getAreaId())) {
            throw new br.com.fiap.cuidar.exception.AreaCuidadoNaoHabilitadaException();
        }
    }
    public List<RegistroCuidado> listar() { return dao.listar(); }
    public RegistroCuidado buscarPorId(Long id) {
        return dao.buscarPorId(id).orElseThrow(RegistroCuidadoNaoEncontradoException::new);
    }
    public RegistroCuidado atualizar(Long id, RegistroCuidado area) {
        buscarPorId(id);
        validarAreaHabilitada(area);
        area.setId(id);
        if (!dao.atualizar(area)) { throw new RegistroCuidadoNaoEncontradoException(); }
        return area;
    }
    public void excluir(Long id) {
        if (!dao.excluir(id)) { throw new RegistroCuidadoNaoEncontradoException(); }
    }
}

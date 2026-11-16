package br.com.fiap.cuidar.service;

import br.com.fiap.cuidar.dao.AreaCuidadoDAO;
import br.com.fiap.cuidar.exception.AreaCuidadoNaoEncontradaException;
import br.com.fiap.cuidar.models.AreaCuidado;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AreaCuidadoService {
    private final AreaCuidadoDAO dao;

    public AreaCuidadoService(AreaCuidadoDAO dao) { this.dao = dao; }

    public AreaCuidado cadastrar(AreaCuidado area) { return dao.cadastrar(area); }
    public List<AreaCuidado> listar() { return dao.listar(); }
    public AreaCuidado buscarPorId(Long id) {
        return dao.buscarPorId(id).orElseThrow(AreaCuidadoNaoEncontradaException::new);
    }
    public AreaCuidado atualizar(Long id, AreaCuidado area) {
        area.setId(id);
        if (!dao.atualizar(area)) { throw new AreaCuidadoNaoEncontradaException(); }
        return area;
    }
    public void excluir(Long id) {
        if (!dao.excluir(id)) { throw new AreaCuidadoNaoEncontradaException(); }
    }
}

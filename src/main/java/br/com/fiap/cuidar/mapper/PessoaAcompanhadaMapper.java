package br.com.fiap.cuidar.mapper;
import br.com.fiap.cuidar.dto.PessoaAcompanhadaRequest;
import br.com.fiap.cuidar.dto.PessoaAcompanhadaResponse;
import br.com.fiap.cuidar.models.PessoaAcompanhada;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class PessoaAcompanhadaMapper {
    private final ModelMapper mapper;

    public PessoaAcompanhadaMapper(ModelMapper mapper) { this.mapper = mapper; }

    public PessoaAcompanhada toEntity(PessoaAcompanhadaRequest request) {
        PessoaAcompanhada pessoa = mapper.map(request, PessoaAcompanhada.class);
        pessoa.setId(null);
        return pessoa;
    }

    public PessoaAcompanhadaResponse toResponse(PessoaAcompanhada pessoa) {
        return mapper.map(pessoa, PessoaAcompanhadaResponse.class);
    }
}

package br.com.fiap.cuidar.mapper;
import br.com.fiap.cuidar.dto.PessoaIdosaRequest;
import br.com.fiap.cuidar.dto.PessoaIdosaResponse;
import br.com.fiap.cuidar.models.PessoaIdosa;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class PessoaIdosaMapper {
    private final ModelMapper mapper;

    public PessoaIdosaMapper(ModelMapper mapper) { this.mapper = mapper; }

    public PessoaIdosa toEntity(PessoaIdosaRequest request) {
        PessoaIdosa pessoa = mapper.map(request, PessoaIdosa.class);
        pessoa.setId(null);
        return pessoa;
    }

    public PessoaIdosaResponse toResponse(PessoaIdosa pessoa) {
        return mapper.map(pessoa, PessoaIdosaResponse.class);
    }
}

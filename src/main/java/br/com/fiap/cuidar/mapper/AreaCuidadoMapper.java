package br.com.fiap.cuidar.mapper;

import br.com.fiap.cuidar.dto.AreaCuidadoRequest;
import br.com.fiap.cuidar.dto.AreaCuidadoResponse;
import br.com.fiap.cuidar.models.AreaCuidado;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class AreaCuidadoMapper {
    private final ModelMapper mapper;

    public AreaCuidadoMapper(ModelMapper mapper) { this.mapper = mapper; }

    public AreaCuidado toEntity(AreaCuidadoRequest request) {
        AreaCuidado area = mapper.map(request, AreaCuidado.class);
        area.setId(null);
        return area;
    }

    public AreaCuidadoResponse toResponse(AreaCuidado area) {
        return mapper.map(area, AreaCuidadoResponse.class);
    }
}

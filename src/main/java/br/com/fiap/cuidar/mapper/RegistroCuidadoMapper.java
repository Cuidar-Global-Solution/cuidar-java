package br.com.fiap.cuidar.mapper;

import br.com.fiap.cuidar.dto.RegistroCuidadoRequest;
import br.com.fiap.cuidar.dto.RegistroCuidadoResponse;
import br.com.fiap.cuidar.models.RegistroCuidado;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class RegistroCuidadoMapper {
    public RegistroCuidado toEntity(br.com.fiap.cuidar.dto.RegistroCuidadoAninhadoRequest request) {
        RegistroCuidado registro = mapper.map(request, RegistroCuidado.class);
        registro.setId(null);
        return registro;
    }
    private final ModelMapper mapper;

    public RegistroCuidadoMapper(ModelMapper mapper) {
        this.mapper = mapper;
        mapper.emptyTypeMap(RegistroCuidadoRequest.class, RegistroCuidado.class)
                .addMappings(mapping -> mapping.skip(RegistroCuidado::setId)).implicitMappings();
        mapper.emptyTypeMap(br.com.fiap.cuidar.dto.RegistroCuidadoAninhadoRequest.class, RegistroCuidado.class)
                .addMappings(mapping -> mapping.skip(RegistroCuidado::setId)).implicitMappings();
    }

    public RegistroCuidado toEntity(RegistroCuidadoRequest request) {
        RegistroCuidado area = mapper.map(request, RegistroCuidado.class);
        area.setId(null);
        return area;
    }

    public RegistroCuidadoResponse toResponse(RegistroCuidado area) {
        return mapper.map(area, RegistroCuidadoResponse.class);
    }
}

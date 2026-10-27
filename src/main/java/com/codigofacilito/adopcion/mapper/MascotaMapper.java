package com.codigofacilito.adopcion.mapper;

import com.codigofacilito.adopcion.dto.MascotaRequest;
import com.codigofacilito.adopcion.dto.MascotaResponse;
import com.codigofacilito.adopcion.dto.TipoMascotaResponse;
import com.codigofacilito.adopcion.entity.Mascota;
import com.codigofacilito.adopcion.entity.TipoMascota;
import org.springframework.stereotype.Component;

@Component
public class MascotaMapper {

    public Mascota toEntity(MascotaRequest request, TipoMascota tipoMascota) {
        return new Mascota(request.nombre().trim(), tipoMascota, request.edad());
    }

    public MascotaResponse toResponse(Mascota mascota) {
        TipoMascota tipo = mascota.getTipoMascota();
        return new MascotaResponse(
                mascota.getId(),
                mascota.getNombre(),
                new TipoMascotaResponse(tipo.getId(), tipo.getNombre()),
                mascota.getEdad(),
                mascota.getDisponible());
    }
}

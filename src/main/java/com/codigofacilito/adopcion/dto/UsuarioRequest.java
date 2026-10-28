package com.codigofacilito.adopcion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos de un usuario adoptante")
public record UsuarioRequest(
        @Schema(description = "Nombre completo", example = "Ana Pérez")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Schema(description = "Correo único; se compara sin distinguir mayúsculas. Los espacios iniciales y "
                + "finales se eliminan antes de validar; los espacios internos se rechazan.",
                example = "ana.perez@mail.com")
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no tiene un formato válido")
        @Size(max = 150, message = "El correo electrónico no puede superar los 150 caracteres")
        String correoElectronico,

        @Schema(description = "Entre 7 y 20 caracteres: dígitos, espacios, paréntesis, guiones y un + inicial opcional",
                example = "+54 11 5555-1234")
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$",
                message = "El teléfono debe tener entre 7 y 20 caracteres: dígitos, espacios, paréntesis, guiones y un + inicial opcional")
        String telefono
) {

    /**
     * Se ejecuta al deserializar el JSON, antes de Bean Validation: así @Email y la búsqueda de
     * duplicados reciben el correo sin espacios en los extremos.
     */
    public UsuarioRequest {
        if (correoElectronico != null) {
            correoElectronico = correoElectronico.strip();
        }
    }
}

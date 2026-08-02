package com.booksaw.betterTeams.luniatic.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El color del menu de clanes.
 *
 * <p>Existe por dos defectos reales que llegaron al servidor: primero los codigos
 * salieron escritos como texto en los items, y despues —ya convertidos— toda la
 * paleta se aplastaba al mismo azul porque el serializador no emitia hex.
 */
@DisplayName("Color del menu de clanes")
class MenusColorTest {

	@Test
	@DisplayName("un hex de la paleta sale como color hex, no como texto")
	void hexSaleComoColor() {
		String salida = Menus.col("&#9235FFHola");

		assertThat(salida).doesNotContain("&#");
		assertThat(salida).contains("Hola");
		// El formato hex legacy es la seccion, una x y los seis digitos intercalados.
		assertThat(salida).containsIgnoringCase("§x");
	}

	@Test
	@DisplayName("dos colores distintos de la paleta no terminan siendo el mismo")
	void laPaletaNoSeAplasta() {
		String marca = Menus.col("&#9235FFa");
		String cuerpo = Menus.col("&#E4D9FFa");
		String etiqueta = Menus.col("&#7162FFa");

		assertThat(marca).isNotEqualTo(cuerpo);
		assertThat(marca).isNotEqualTo(etiqueta);
		assertThat(cuerpo).isNotEqualTo(etiqueta);
	}

	@Test
	@DisplayName("el MiniMessage que traen los nombres de clan tambien se resuelve")
	void miniMessageSeResuelve() {
		String salida = Menus.col("<yellow>Admins</yellow>");

		assertThat(salida).doesNotContain("<yellow>");
		assertThat(salida).doesNotContain("</yellow>");
		assertThat(salida).contains("Admins");
		assertThat(salida).contains("§");
	}

	@Test
	@DisplayName("los codigos legacy de siempre siguen funcionando")
	void legacySigueAndando() {
		assertThat(Menus.col("&cError")).isEqualTo("§cError");
	}

	@Test
	@DisplayName("un texto sin color no gana basura")
	void textoPeladoQuedaIgual() {
		assertThat(Menus.col("Clanes")).isEqualTo("Clanes");
	}
}

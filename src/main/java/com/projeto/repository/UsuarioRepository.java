package com.projeto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projeto.entity.Usuario;
import com.projeto.entity.enums.Perfil;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByEmailIgnoreCase(String email);

	@Query("""
			select u from Usuario u
			left join fetch u.restaurante r
			left join fetch r.admin
			where u.id = :id
			""")
	Optional<Usuario> findParaEscopo(@Param("id") Long id);

	Optional<Usuario> findByTokenAcesso(String tokenAcesso);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByPerfil(Perfil perfil);

	List<Usuario> findAllByOrderByEmailAsc();

	List<Usuario> findByPerfilOrderByNomeAsc(Perfil perfil);

	List<Usuario> findByRestauranteIdOrderByNomeAsc(Long restauranteId);

	List<Usuario> findByRestauranteIdAndPerfilOrderByNomeAsc(Long restauranteId, Perfil perfil);

	@Query("select u from Usuario u where u.restaurante.admin.id = :adminId order by u.nome asc")
	List<Usuario> findDaEquipeDoAdmin(@Param("adminId") Long adminId);
}

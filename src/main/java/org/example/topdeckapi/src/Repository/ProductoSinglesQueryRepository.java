package org.example.topdeckapi.src.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.example.topdeckapi.src.model.Producto;
import org.hibernate.query.NativeQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ProductoSinglesQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<Producto> buscarSingles(
            String searchPattern,
            Long idTag,
            Map<String, List<String>> atributos,
            String sortColumn,
            boolean ascending,
            int page,
            int size) {

        StringBuilder from = new StringBuilder(
                "FROM producto p INNER JOIN producto_singles ps ON ps.id_producto = p.id_producto " +
                "WHERE p.tipo_producto = 'SINGLE' AND p.activo = true");

        if (searchPattern != null) {
            from.append(" AND LOWER(p.nombre) LIKE :searchPattern");
        }
        if (idTag != null) {
            from.append(" AND p.id_tag = :idTag");
        }

        int i = 0;
        if (atributos != null) {
            for (Map.Entry<String, List<String>> entry : atributos.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                String k = "k" + i;
                String v = "v" + i;
                from.append(" AND EXISTS (SELECT 1 FROM jsonb_each_text(ps.atributos) e")
                        .append(" WHERE e.key = :").append(k)
                        .append(" AND e.value IN (:").append(v).append("))");
                i++;
            }
        }

        String safeColumn = (sortColumn == null || sortColumn.isBlank()) ? "uuid" : sortColumn;
        String direction = ascending ? "ASC" : "DESC";

        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(*) " + from);

        NativeQuery<?> dataQuery = entityManager
                .createNativeQuery("SELECT p.* " + from + " ORDER BY p." + safeColumn + " " + direction)
                .unwrap(NativeQuery.class);
        dataQuery.addEntity(Producto.class);
        dataQuery.setFirstResult(page * size);
        dataQuery.setMaxResults(size);

        if (searchPattern != null) {
            countQuery.setParameter("searchPattern", searchPattern);
            dataQuery.setParameter("searchPattern", searchPattern);
        }
        if (idTag != null) {
            countQuery.setParameter("idTag", idTag);
            dataQuery.setParameter("idTag", idTag);
        }

        int j = 0;
        if (atributos != null) {
            for (Map.Entry<String, List<String>> entry : atributos.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                String k = "k" + j;
                String v = "v" + j;
                countQuery.setParameter(k, entry.getKey());
                dataQuery.setParameter(k, entry.getKey());
                countQuery.setParameter(v, entry.getValue());
                dataQuery.setParameter(v, entry.getValue());
                j++;
            }
        }

        long total = ((Number) countQuery.getSingleResult()).longValue();
        @SuppressWarnings("unchecked")
        List<Producto> contenido = (List<Producto>) dataQuery.getResultList();

        return new PageImpl<>(contenido, PageRequest.of(page, size), total);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerFacetas(Long idTag, List<String> denylist) {
        StringBuilder sql = new StringBuilder(
                "SELECT e.key AS atributo, e.value AS valor, COUNT(*) AS cantidad " +
                "FROM producto p " +
                "INNER JOIN producto_singles ps ON ps.id_producto = p.id_producto " +
                "CROSS JOIN LATERAL jsonb_each_text(ps.atributos) e " +
                "WHERE p.tipo_producto = 'SINGLE' AND p.activo = true " +
                "AND e.value IS NOT NULL AND e.value <> '' " +
                "AND e.key NOT IN (:denylist) ");

        if (idTag != null) {
            sql.append("AND p.id_tag = :idTag ");
        }
        sql.append("GROUP BY e.key, e.value ORDER BY e.key ASC, COUNT(*) DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("denylist", denylist);
        if (idTag != null) {
            query.setParameter("idTag", idTag);
        }
        return query.getResultList();
    }
}

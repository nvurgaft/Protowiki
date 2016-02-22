package com.protowiki.model;

import com.protowiki.beans.Author;
import com.protowiki.entities.RDFStatement;
import com.protowiki.utils.RDFUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author Nick
 */
public class AuthorModel {

    public static Logger logger = LoggerFactory.getLogger(AuthorModel.class);

    private static final String GRAPH_NAME = "http://authors";
    
    private VirtGraph graph;
    private QueryHandler qh;
    
    public AuthorModel() {
        this.graph = new RDFConnectionManager(GRAPH_NAME).getGraphConnection(false);
        this.qh = new QueryHandler(graph, GRAPH_NAME);
    }

    /**
     *  Clears the authors graph
     * @return
     */
    public boolean clearGraph() {
        return qh.clearGraph();
    }

    /**
     *
     * @param authorsList
     * @return was the transaction successful
     */
    public boolean insertAuthorsIntoDB(List<Author> authorsList) {

        if (authorsList == null || authorsList.isEmpty()) {
            return false;
        }

        try {
            authorsList.stream().forEach(author -> {
                // build a statement model
                List<RDFStatement> stmts = new ArrayList<>();
                stmts.add(new RDFStatement(author.getMarcId(), "rdf:url", author.getWikipediaUri()));
                stmts.add(new RDFStatement(author.getMarcId(), "rdf:enName", author.getNames().get("en")));
                stmts.add(new RDFStatement(author.getMarcId(), "rdf:heName", author.getNames().get("he")));
                stmts.add(new RDFStatement(author.getMarcId(), "rdf:viaf", author.getViafId()));
                stmts.add(new RDFStatement(author.getMarcId(), "rdf:nli", author.getNliId()));
                qh.batchInsertStatements(stmts);
            });
        } catch (Exception ex) {
            logger.error("Exception while attempting to batch insert authors into DB");
            return false;
        }
        return true;
    }

    /**
     * Takes a String map of viaf ids to abstracts and stores it in the
     * database model
     *
     * @param abstractsMap
     * @return true if and only is the data insertion was successful
     */
    public boolean insertAuthorsViafAndAbstracts(Map<String, String> abstractsMap) {
        if (abstractsMap == null || abstractsMap.isEmpty()) {
            return false;
        }
        try {
            List<RDFStatement> stmts = new ArrayList<>();
            for (String key : abstractsMap.keySet()) {
                stmts.add(new RDFStatement(key, "rdf:abstract", abstractsMap.get(key)));
            }
            qh.batchInsertStatements(stmts);
        } catch (Exception ex) {
            logger.error("Exception while inserting abstracts to model", ex);
        }
        return true;
    }
    
    /**
     * Takes a pair of a viaf id and an abstract and stores it in the
     * database model
     *
     * @param viaf
     * @param articleAbstract
     * @return true if and only is the data insertion was successful
     */
    public boolean insertAuthorsViafAndAbstracts(String viaf, String articleAbstract) {
        if (viaf == null || viaf.isEmpty()) {
            return false;
        }
        if (articleAbstract == null || articleAbstract.isEmpty()) {
            return false;
        }
        try {
            logger.info("Inserting viaf: " + viaf + ", and abstract: " + articleAbstract);
            qh.insertStatement(viaf, "rdf:abstract",  RDFUtils.escapeTextLiteral(articleAbstract));
        } catch (Exception ex) {
            logger.error("Exception while inserting abstracts to model", ex);
            return false;
        }
        return true;
    }

    /**
     *
     * @return a map of viad id keys and abstracts values
     */
    public Map<String, String> getAuthorsViafAndAbstracts() {
        List<RDFStatement> resultList = null;
        Map<String, String> resultMap = new HashMap<>();
        try {
            resultList = qh.selectTriples("?viaf", "rdf:abstract", "?abstract");
            resultList.stream().forEach(r -> {
                resultMap.put(r.getSubject().trim(), r.getObject().trim());
            });
        } catch (Exception ex) {
            logger.error("Exception while fetching author abstracts from model", ex);
        }
        return resultMap;
    }
}
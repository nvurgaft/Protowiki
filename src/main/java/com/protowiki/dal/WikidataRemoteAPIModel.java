package com.protowiki.dal;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.protowiki.beans.Author;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

/**
 *
 * @author Nick
 */
public class WikidataRemoteAPIModel {

    public static Logger logger = LoggerFactory.getLogger(WikidataRemoteAPIModel.class);

    static String connection_string = "jdbc:virtuoso://localhost:1111/CHARSET=UTF-8/log_enable=2";
    static String login = "dba";
    static String password = "dba";

    private static final String GET_WIKIPEDIA_ABSTRACT_USING_LABEL = StringUtils.join(
            new String[]{
                "PREFIX ontology: <http://dbpedia.org/ontology/>",
                "PREFIX property: <http://dbpedia.org/property/>",
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                "SELECT ?name ?abstract WHERE {",
                "?name rdfs:label \"%s\"@%s.",
                "?name ontology:abstract ?abstract",
                "FILTER (LANG(?abstractEn)='%s')",
                "}"
            }, "\n");

    private static final String GET_WIKIPEDIA_ABSTRACT_USING_VIAF = StringUtils.join(
            new String[]{
                "PREFIX ontology: <http://dbpedia.org/ontology/>",
                "PREFIX property: <http://dbpedia.org/property/>",
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                "SELECT ?name ?abstract WHERE {",
                "?name <http://dbpedia.org/property/viaf> \"%s\"^^<http://www.w3.org/2001/XMLSchema#integer> .",
                "?name ontology:abstract ?abstract",
                "FILTER (LANG(?abstractEn)='%s')",
                "}"
            }, "\n"
    );

    private static final String GET_VIAF_FROM_HEBREW_AUTHORS = StringUtils.join(
            new String[]{
                "PREFIX wikibase: <http://wikiba.se/ontology#>",
                "PREFIX wd: <http://www.wikidata.org/entity/>",
                "PREFIX wdt: <http://www.wikidata.org/prop/direct/>",
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                "PREFIX bd: <http://www.bigdata.com/rdf#>",
                "SELECT ?s ?viaf ?nli ?enName ?heName WHERE {",
                "?s wdt:P31 wd:Q5 .",
                "?s wdt:P949 ?nli .",
                "?s wdt:P214 $viaf .",
                "SERVICE wikibase:label {",
                "bd:serviceParam wikibase:language \"he\" .",
                "?s rdfs:label ?heName .",
                "}",
                "SERVICE wikibase:label {",
                "bd:serviceParam wikibase:language \"en\" .",
                "?s rdfs:label ?enName .",
                "}",
                "}"
            }, "\n");

    /**
     * Runs a user provided SPARQL query on the Wikidata endpoint
     *
     * @param queryString The query string
     * @param sparqlServiceUrl The SPARQL endpoint ULR, if null or empty string,
     * will default to "https://query.wikidata.org/sparql"
     * @return the query results in a string
     */
    public String runQueryOnWikidata(String queryString, String sparqlServiceUrl) {
        if (queryString == null || queryString.isEmpty()) {
            return null;
        }

        if (sparqlServiceUrl == null || sparqlServiceUrl.isEmpty()) {
            sparqlServiceUrl = "https://query.wikidata.org/sparql";
        }

        StringBuilder sb = new StringBuilder();
        try {
            Query query = QueryFactory.create(GET_VIAF_FROM_HEBREW_AUTHORS);
            QueryExecution qe = QueryExecutionFactory.sparqlService(sparqlServiceUrl, query);
            ResultSet rs = ResultSetFactory.copyResults(qe.execSelect());
            while (rs.hasNext()) {
                QuerySolution qs = rs.next();
                Iterator<String> iter = qs.varNames();
                sb.append(StringUtils.repeat("-", 50)).append("\n");
                while (iter.hasNext()) {
                    String field = iter.next();
                    sb.append(field).append(": ").append(qs.get(field)).append("\n");
                }
            }
        } catch (Exception ex) {
            logger.error("An exception has occured while processing the query", ex);
            return new StringBuilder()
                    .append("An exception has occured while processing the query")
                    .append("\n")
                    .append(ex)
                    .append("\n")
                    .toString();
        }
        return sb.toString();
    }

    /**
     * Fetches (by label) from DBPedia the article abstract for an author in a
     * certain language
     *
     * @param author The queried author
     * @param language The language that abstract should be in (if string is
     * null or empty, will default to 'en')
     *
     * @return The article abstract text string
     */
    public String getWikipediaAbstractByName(String author, String language) {
        if (author == null || author.isEmpty()) {
            return null;
        }
        if (language == null || language.isEmpty()) {
            language = "en";
        }

        String queryString = String.format(GET_WIKIPEDIA_ABSTRACT_USING_LABEL, author, language, language);
        Query query = QueryFactory.create(queryString);
        QueryExecution qe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
        String result = null;
        ResultSet rs = ResultSetFactory.copyResults(qe.execSelect());
        while (rs.hasNext()) {
            QuerySolution qs = rs.nextSolution();
            RDFNode _name = qs.get("name");
            RDFNode _abstract = qs.get("abstract");
            logger.info("Name: " + _name + ", Abstract: " + _abstract);
            result = _abstract.toString();
        }
        return result;
    }
    
    /**
     * Fetches (by viaf id) from DBPedia the article abstract for an author in 
     * a certain language
     *
     * @param author The queried author
     * @param language The language that abstract should be in (if string is
     * null or empty, will default to 'en')
     *
     * @return The article abstract text string
     */
    public String getWikipediaAbstractByViafId(String author, String language) {
        if (author == null || author.isEmpty()) {
            return null;
        }
        if (language == null || language.isEmpty()) {
            language = "en";
        }

        String queryString = String.format(GET_WIKIPEDIA_ABSTRACT_USING_VIAF, author, language, language);
        Query query = QueryFactory.create(queryString);
        QueryExecution qe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
        String result = null;
        ResultSet rs = ResultSetFactory.copyResults(qe.execSelect());
        while (rs.hasNext()) {
            QuerySolution qs = rs.nextSolution();
            RDFNode _name = qs.get("name");
            RDFNode _abstract = qs.get("abstract");
            logger.info("Name: " + _name + ", Abstract: " + _abstract);
            result = _abstract.toString();
        }
        return result;
    }

    /**
     * Fetches from local VOS all authors who have a VIAF ID and an NLI ID.
     * These response is built in the following format (columns per row): 1. s -
     * Subject 2. sLabel - Subject label 3. viaf - VIAF ID 4. nli - NLI ID
     *
     * @return A list of Author objects
     */
    public List<Author> getAuthorsWithVIAF() {
        List<Author> authors = null;
        try {
            VirtGraph graph = new VirtGraph(connection_string, login, password);

            Query query = QueryFactory.create(GET_VIAF_FROM_HEBREW_AUTHORS);
            VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(query, graph);
            ResultSet rs = vqe.execSelect();
            authors = new ArrayList<>();
            while (rs.hasNext()) {
                QuerySolution qs = rs.nextSolution();
                RDFNode _s = qs.get("s");
                RDFNode _viaf = qs.get("viaf");
                RDFNode _nli = qs.get("nli");
                RDFNode _enName = qs.get("enName");
                RDFNode _heName = qs.get("heName");
                Author author = new Author();
                author.setWikipediaUri(_s.toString());
                author.setViafId(_viaf.toString());
                author.setNliId(_nli.toString());
                author.setNames(new HashMap<>());
                author.getNames().put("en", _enName.toString());
                author.getNames().put("he", _heName.toString());
                authors.add(author);
                logger.debug("s: " + _s + ", VIAF ID: " + _viaf + ", NLI: " + _nli + ", English name: " + _enName + ", Hebrew name: " + _heName);
            }
        } catch (Exception ex) {
            logger.error("Exception while marshalling authors list from local VOS RDF", ex);
        }

        return authors;
    }

    /**
     * Fetches from Wikidata all authors who have a VIAF ID and an NLI ID.
     * Response is built in the following format (columns per row): 1. s -
     * Subject 2. viaf - VIAF ID 3. nli - NLI ID 4. enName - Author full name in
     * English (defaults to Q id is none was provided) 5. heName - Author full
     * name in Hebrew (defaults to Q id is none was provided)
     *
     * @return A list of Author objects
     */
    public List<Author> getAuthorsWithVIAFRemote() {

        List<Author> authors = null;
        try {
            Query query = QueryFactory.create(GET_VIAF_FROM_HEBREW_AUTHORS);

            QueryExecution qe = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", query);
            ResultSet rs = ResultSetFactory.copyResults(qe.execSelect());
            authors = new ArrayList<>();
            while (rs.hasNext()) {
                QuerySolution qs = rs.nextSolution();
                RDFNode _s = qs.get("s");
                RDFNode _viaf = qs.get("viaf");
                RDFNode _nli = qs.get("nli");
                RDFNode _enName = qs.get("enName");
                RDFNode _heName = qs.get("heName");
                Author author = new Author();
                author.setWikipediaUri(_s.toString());
                author.setViafId(_viaf.toString());
                author.setNliId(_nli.toString());
                author.setNames(new HashMap<>());
                author.getNames().put("en", _enName.toString());
                author.getNames().put("he", _heName.toString());
                authors.add(author);
                logger.debug("s: " + _s + ", VIAF ID: " + _viaf + ", NLI: " + _nli + ", English name: " + _enName + ", Hebrew name: " + _heName);
            }
        } catch (Exception ex) {
            logger.error("Exception while marshalling authors list from remote SPARQL API", ex);
        }

        return authors;
    }
}
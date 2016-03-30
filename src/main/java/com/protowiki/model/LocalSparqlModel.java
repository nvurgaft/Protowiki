package com.protowiki.model;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.protowiki.beans.Author;
import com.protowiki.utils.DatabaseProperties;
import com.protowiki.values.Prefixes;
import java.util.ArrayList;
import java.util.HashMap;
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
public class LocalSparqlModel {

    public static Logger logger = LoggerFactory.getLogger(LocalSparqlModel.class);

    private DatabaseProperties dbProps;

    private String connection_string;
    private String login;
    private String password;

    private static final String GET_VIAF_FROM_HEBREW_AUTHORS = StringUtils.join(
            new String[]{
                Prefixes.WIKIBASE, Prefixes.WD, Prefixes.WDT, Prefixes.RDFS, Prefixes.BD,
                "SELECT ?s ?viaf ?nli ?enName ?heName WHERE {",
                "?s wdt:P31 wd:Q5 .",
                "?s wdt:P949 ?nli .",
                "?s wdt:P214 $viaf .",
                "SERVICE wikibase:label {",
                "bd:serviceParam wikibase:language 'he' .",
                "?s rdfs:label ?heName .",
                "}",
                "SERVICE wikibase:label {",
                "bd:serviceParam wikibase:language 'en' .",
                "?s rdfs:label ?enName .",
                "}",
                "}"
            }, "\n");

    public LocalSparqlModel() {
        this.dbProps = new DatabaseProperties("application.properties");

        login = this.dbProps.getProperty("login");
        password = this.dbProps.getProperty("password");
        String host = this.dbProps.getProperty("host");
        String port = this.dbProps.getProperty("port");

        //connection_string = "jdbc:virtuoso://localhost:1111/CHARSET=UTF-8/log_enable=2";
        connection_string = new StringBuilder().append("jdbc:virtuoso://").append(host).append(":").append(port).append("/CHARSET=UTF-8/log_enable=2").toString();
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
            }
        } catch (Exception ex) {
            logger.error("Exception while marshalling authors list from local VOS RDF", ex);
        }
        return authors;
    }
}

package com.protowiki.core;

import com.protowiki.beans.Author;
import com.protowiki.beans.Record;
import com.protowiki.model.AuthorModel;
import com.protowiki.model.WikidataRemoteAPIModel;
import com.protowiki.utils.RecordSAXParser;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  These tests act as Integration tests between the various components in this
 *  application and represent the eventual business logic.
 *  These tests should be run separately from the build process.
 * 
 * @author Nick
 */
@Ignore
public class ProcessTest {
    
    public static Logger logger = LoggerFactory.getLogger(ProcessTest.class);

    private static final String FILE_PATH = "C://files//authbzi.xml";

    @Test
    public void testFetchRemoteAbstracts() {
        RecordSAXParser parser = new RecordSAXParser();
        DataTransformer optimus = new DataTransformer();
        List<Record> records = parser.parseXMLFileForRecords(FILE_PATH);
        List<Author> authorsList = optimus.transformRecordsListToAuthors(records);
        logger.info("scan the authors list and get a list of viaf ids");
        List<String> viafs = authorsList.stream().filter(a -> {
            return a.getViafId() != null;
        }).map(a -> {
            System.out.println(a.getViafId());
            return a.getViafId().trim();
        }).collect(Collectors.toList());
        logger.info("connect remotly and query abstracts for these viaf ids");
        WikidataRemoteAPIModel remoteApi = new WikidataRemoteAPIModel();
        Map<String, String> absMap = remoteApi.getMultipleWikipediaAbstractByViafIds(viafs, "en");
        logger.info("insert locally");
        AuthorModel authorModel = new AuthorModel();
        
        for (String key : absMap.keySet()) {
            authorModel.insertAuthorsViafAndAbstracts(key, absMap.get(key));
        }
    }

    @Test
    public void testEntireProcess() {
        DataTransformer optimus = new DataTransformer();

        logger.info("connect remotly and query abstracts for these viaf ids");
        AuthorModel authorModel = new AuthorModel();
        Map<String, String> rdfList = authorModel.getAuthorsViafAndAbstracts();

        logger.info("generate the updated MARC file");
        boolean result = optimus.generateMARCXMLFile(FILE_PATH, rdfList);
        assertTrue("Should be successful", result);
    }
}
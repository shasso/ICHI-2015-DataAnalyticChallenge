/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hcdachallengeBuildIndex;

import CustomAnalyzers.CustomAnalyzers;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import QuestionsParser.Question;
import QuestionsParser.processQuestionsDocument;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Properties;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author sargon.hasso
 */
public class HcDAChallengeBuildIndex {

    private static final Logger LOGGER = Logger.getLogger(HcDAChallengeBuildIndex.class.getName());
    private static Handler fileHandler = null;
    private static SimpleFormatter simpleFormatter = null;

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {

        String InputFile = null;    // corpus
        String OutputFile = null;
        String indexDir = null;
        String dataDir = null;
        String wordnetfile = null;
        String propertiesFile = null;
        String LogFilePath = null;
        String LogFileName = null;

        boolean fInputFile = false;
        boolean fOutputFile = false;
        boolean idxFlag = false;
        boolean dataDirFlag = false;
        boolean OverWrite = false;
        boolean fUseSynonyms = false;
        boolean fdataDir = false;
        boolean fConfigFile = false;
        boolean fLogFilePath = false;

        // process command line options
        for (int i = 0; i < args.length; i++) {
            if (null != args[i]) {
                switch (args[i]) {
                    case "-c":
                        propertiesFile = args[i + 1];
                        fConfigFile = true;
                        i++;
                        break;

                    case "-h":
                        usage();
                        System.exit(1);
                }
            }
        }

        // check proper usage
        StringBuilder usageError = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        boolean mustExit = false;
        if (!fConfigFile) {
            usageError.append("-c properties file is required");

            usageError.append(NEW_LINE);
            usage(usageError.toString());
            System.exit(1);
        }
        // read properties file
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(propertiesFile);
            try {
                // load a properties file
                prop.load(input);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }

            dataDir = prop.getProperty("DATA_Path");
            fdataDir = dataDir != null;

            indexDir = prop.getProperty("IDX_Path");
            idxFlag = indexDir != null;

            InputFile = prop.getProperty("Corpus_File");
            fInputFile = InputFile != null;

            wordnetfile = prop.getProperty("Wordnet_File");
            fUseSynonyms = wordnetfile != null;

            String param = prop.getProperty("overwrite", "y");
            if (param.equalsIgnoreCase("y")) {
                OverWrite = true;
            }

            LogFilePath = prop.getProperty("LogFile_Path");
            fLogFilePath = LogFilePath != null;
            DateFormat df = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss");
            Calendar calobj = Calendar.getInstance();
            String suffix = df.format(calobj.getTime()) + ".log";
            if (fLogFilePath) {
                LogFileName = LogFilePath + File.separator + suffix;
            } else {
                LogFileName = suffix;
            }

            // do some initilaization
            // Creating FileHandler
            fileHandler = new FileHandler(LogFileName);
            // Creating SimpleFormatter
            simpleFormatter = new SimpleFormatter();
            // Assigning handler to logger
            LOGGER.addHandler(fileHandler);
            // Setting formatter to the handler
            fileHandler.setFormatter(simpleFormatter);
            // Setting Level to ALL
            fileHandler.setLevel(Level.ALL);
            LOGGER.setLevel(Level.ALL);

            // log all applicaion parameters
            LOGGER.info("application parameters");
            LOGGER.log(Level.INFO, "DATA_Path = {0}", dataDir);
            LOGGER.log(Level.INFO, "IDX_Path = {0}", indexDir);
            LOGGER.log(Level.INFO, "Corpus_File = {0}", InputFile);
            LOGGER.log(Level.INFO, "Wordnet_File = {0}", wordnetfile);
            LOGGER.log(Level.INFO, "overwrite = {0}", OverWrite);
            LOGGER.log(Level.INFO, "LogFile_Path = {0}", LogFilePath);
            LOGGER.log(Level.INFO, "LogFileName = {0}", LogFileName);
            

            // get the property value and print it out
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        if (!fInputFile) {
            usageError.append("Corpus_File value in properties file is required");
            mustExit = true;
        }
        if (!idxFlag) {
            usageError.append("-x index directory is required");
            mustExit = true;
        }
        if (mustExit) {
            usageError.append(NEW_LINE);
            usage(usageError.toString());
            System.exit(1);
        }

        // process input data
        processQuestionsDocument p = new processQuestionsDocument(InputFile);
        ArrayList<Question> qList = p.InputToDocumentsList();
        int count = p.NumProcessedLines();
        System.out.println("questions processed: " + count);
        LOGGER.log(Level.INFO, "questions processed: {0}", count);

        // create IndexWriter 
        HcDAChallengeBuildIndex indexer = null;
        try {
            if (fUseSynonyms) {
                indexer = new HcDAChallengeBuildIndex(indexDir, OverWrite, wordnetfile);
            } else {
                indexer = new HcDAChallengeBuildIndex(indexDir, OverWrite);
            }

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        try {
            // start indexing
            // close the index writer

            int j;
            j = indexer.index(qList);
            // System.out.println("questions indexed: " + j);
            LOGGER.log(Level.INFO, "questions indexed: {0}", j);

            indexer.close();    // close index writer
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private static void usage(String errMsg) {

        System.err.println(errMsg);
        usage();
    }

    private static void usage() {
        String msg = HcDAChallengeBuildIndex.class.getName()
                + " [-c properties file (full path name)] "
                + "[-h Print this usage information]";
        System.err.println("Usage: " + msg);
    }

    private IndexWriter writer;

    public HcDAChallengeBuildIndex(String indexDir, boolean overwrite) throws IOException {
        Directory dir = FSDirectory.open(Paths.get(indexDir));

        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        if (overwrite) {
            // Create a new index in the directory, removing any
            // previously indexed documents:
            iwc.setOpenMode(OpenMode.CREATE);
        } else {
            // Add new documents to an existing index:
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        }

        // Optional: for better indexing performance, if you
        // are indexing many documents, increase the RAM
        // buffer.  But if you do this, increase the max heap
        // size to the JVM (eg add -Xmx512m or -Xmx1g):
        //
        // iwc.setRAMBufferSizeMB(256.0);
        writer = new IndexWriter(dir, iwc);
    }

    public HcDAChallengeBuildIndex(String indexDir, boolean overwrite, String wordnetfile) throws IOException {
        Directory dir = FSDirectory.open(Paths.get(indexDir));
        CustomAnalyzers ca = CustomAnalyzers.getInstance(wordnetfile);
        Analyzer analyzer = ca.getSynonymAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        if (overwrite) {
            // Create a new index in the directory, removing any
            // previously indexed documents:
            iwc.setOpenMode(OpenMode.CREATE);
        } else {
            // Add new documents to an existing index:
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        }

        // Optional: for better indexing performance, if you
        // are indexing many documents, increase the RAM
        // buffer.  But if you do this, increase the max heap
        // size to the JVM (eg add -Xmx512m or -Xmx1g):
        //
        // iwc.setRAMBufferSizeMB(256.0);
        writer = new IndexWriter(dir, iwc);
    }

    public void close() throws IOException {
        writer.close();
    }

    public int index(ArrayList<Question> qList)
            throws Exception {

        qList.stream().forEach((q) -> {
            try {
                indexFile(q);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
           
        });
        return writer.numDocs();
    }

    private void indexFile(Question q) throws Exception {
        // System.out.println("Indexing " + q.toString());
        LOGGER.log(Level.INFO, "Indexing {0}", q.toString());
        Document doc = getDocument(q);
        writer.addDocument(doc);
    }

    protected Document getDocument(Question q) throws Exception {
        Document doc = new Document();
        FieldType ft = new FieldType();
        ft.setStoreTermVectors(true);
        ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        ft.setStored(true);

        // doc.add(new Field("question", q.Body, Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("question", q.Body, ft));

        // ft.setIndexOptions(IndexOptions.);
        doc.add(new Field("topic", q.Topic, TextField.TYPE_STORED));

//        doc.add(new Field("docID", q.ID, Field.Store.YES, Field.Index.NOT_ANALYZED));
        ft = new FieldType(StringField.TYPE_STORED);

        doc.add(new Field("docID", q.ID, ft));
        return doc;
    }
}

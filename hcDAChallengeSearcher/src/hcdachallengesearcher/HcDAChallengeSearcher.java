/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hcdachallengesearcher;

import CustomAnalyzers.CustomAnalyzers;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import QuestionsParser.Question;
import QuestionsParser.processQuestionsDocument;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author sargon.hasso
 */
public class HcDAChallengeSearcher {

    private static Object TestSimilarityBase;
    private static String SummaryResultsFile = null;
    private static FileWriter writer;
    private static final Logger LOGGER = Logger.getLogger(HcDAChallengeSearcher.class.getName());
    private static Handler fileHandler = null;
    private static SimpleFormatter simpleFormatter = null;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        String InputQuery = null;
        String indexDir = null;
        String InputFile = null;
        String dataDir = null;
        String testDir = null;
        String configFile = null;
        String field = "question";
        int topHits = 3;
        long seed = 10;
        String wordnetfile = null;
        int simIdx = 0;
        int test_id = 1;
        boolean fConfigFile = false;
        String LogFilePath = null;
        String LogFileName = null;

        boolean fInputQuery = false;
        boolean idxFlag = false;
        boolean fProcessQuestionsFromFile = false;
        boolean fRandomize = false;
        boolean fUseSynonyms = false;
        boolean fUseSimilarity = false;
        boolean fTesting = false;
        boolean fdataDir = false;
        boolean ftestDir = false;
        boolean fLogFilePath = false;

        // parameter values for ranking functions
        float k1_parameter = 0;
        float b_parameter = 0;
        float mu_parameter = 0;
        float lambda_parameter = 0;

        // use different ranking functions, i.e. similarity function
        List<Similarity> sims;

        // process command line options
        for (int i = 0; i < args.length; i++) {
            if (null != args[i]) {
                switch (args[i]) {

                    case "-q":
                        InputQuery = args[i + 1];
                        fInputQuery = true;
                        i++;
                        break;

                    case "-c":
                        configFile = args[i + 1];
                        fConfigFile = true;
                        i++;
                        break;
                    // it is convenient someimes to pass this parameter from command line
                    case "-s":
                        simIdx = Integer.parseInt(args[i + 1]);
                        fUseSimilarity = true;
                        i++;
                        break;

                    case "-h":
                        usage();
                        System.exit(1);
                }
            }
        }

        // check proper usage & correct input
        StringBuilder usageError = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        boolean mustExit = false;
        if (!fConfigFile) {
            usageError.append("-c properties file is required");
            mustExit = true;
        }

        String Test_Id = null;
        String sim_index = null;
        String Top_Hits = null;
        String randomize = null;

        // read properties file
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(configFile);
            // load a properties file
            prop.load(input);

            dataDir = prop.getProperty("DATA_Path");
            fdataDir = dataDir != null;

            indexDir = prop.getProperty("IDX_Path");
            idxFlag = indexDir != null;

            testDir = prop.getProperty("TEST_Path");
            ftestDir = testDir != null;

            field = prop.getProperty("Default_Field", "question");

            InputFile = prop.getProperty("Input_File");
            fProcessQuestionsFromFile = InputFile != null;

            wordnetfile = prop.getProperty("Wordnet_File");
            fUseSynonyms = wordnetfile != null;

            Test_Id = prop.getProperty("Test_Id");
            fTesting = Test_Id != null;
            if (fTesting) {
                test_id = Integer.parseInt(Test_Id);
            }

            // if sim idx is not given from command line, read it from prop file
            if (!fUseSimilarity) {
                sim_index = prop.getProperty("Sim_Idx");
                fUseSimilarity = sim_index != null;
                if (fUseSimilarity) {
                    simIdx = Integer.parseInt(sim_index);
                }
            }
            Top_Hits = prop.getProperty("Top_Hits", "3");
            topHits = Integer.parseInt(Top_Hits);

            randomize = prop.getProperty("Seed");
            if (randomize != null) {
                seed = Integer.parseInt(randomize);
                fRandomize = true;
            }

            String parameter;
            parameter = prop.getProperty("k1_parameter", "1.2");
            k1_parameter = Float.parseFloat(parameter);
            parameter = prop.getProperty("b_parameter", "0.75");
            b_parameter = Float.parseFloat(parameter);
            parameter = prop.getProperty("mu_parameter", "2000");
            mu_parameter = Float.parseFloat(parameter);
            parameter = prop.getProperty("lambda_parameter", "0.7");
            lambda_parameter = Float.parseFloat(parameter);

            // get the property value and print it out
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        // continue checking all parameters are correct
        if (!idxFlag) {
            usageError.append("index path is required: IDX_Path in properties file must be set");
            mustExit = true;
        }
        if (fUseSimilarity) {
            if (simIdx < 0 || simIdx > 4) {
                usageError.append("Sim_Idx value must be between 0 & 4 inclusive");
                mustExit = true;
            }
        }
        if (fTesting && !ftestDir) {
            usageError.append("testing option requires setting TEST_Path value in properties file");
            mustExit = true;
        }
        if (!fInputQuery && !fProcessQuestionsFromFile) {
            usageError.append("Either -q value (from command line) or  Input_file (in properties file) must be given");
            mustExit = true;
        }
        if (mustExit) {
            usageError.append(NEW_LINE);
            usage(usageError.toString());
            System.exit(1);
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
        LOGGER.log(Level.INFO, "LogFile_Path = {0}", LogFilePath);
        LOGGER.log(Level.INFO, "LogFileName = {0}", LogFileName);
        LOGGER.log(Level.INFO, "Default_Field = {0}", field);
        LOGGER.log(Level.INFO, "TEST_Path = {0}", testDir);
        LOGGER.log(Level.INFO, "Test_Id = {0}", Test_Id);
        LOGGER.log(Level.INFO, "Sim_Idx = {0}", sim_index);
        LOGGER.log(Level.INFO, "Top_Hits = {0}", Top_Hits);
        LOGGER.log(Level.INFO, "Seed = {0}", randomize);
        LOGGER.log(Level.INFO, "k1_parameter = {0}", k1_parameter);
        LOGGER.log(Level.INFO, "b_parameter = {0}", b_parameter);
        LOGGER.log(Level.INFO, "mu_parameter = {0}", mu_parameter);
        LOGGER.log(Level.INFO, "lambda_parameter = {0}", lambda_parameter);

        // initialize sims
        sims = new ArrayList<>();
        if (fUseSimilarity) {

            sims.add(new DefaultSimilarity());
            sims.add(new BM25Similarity(k1_parameter, b_parameter));

            sims.add(new LMDirichletSimilarity(mu_parameter));
            sims.add(new LMJelinekMercerSimilarity(0.1f));
            sims.add(new LMJelinekMercerSimilarity(lambda_parameter)); // for long queries
        }
        // we will write summary results to file
        // construct file name based on commandline options
        if (fTesting) {
            StringBuilder fname = new StringBuilder();
            if (fUseSynonyms) {
                fname.append("S");
            } else {
                fname.append("F");
            }

            // append sim index
            fname.append(simIdx);
            fname.append("_").append(test_id);
            fname.append(".txt");
            SummaryResultsFile = testDir + File.separator + fname.toString();
            try {
                // create the file
                writer = new FileWriter(SummaryResultsFile, true);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

        try {
            Similarity sim = fUseSimilarity ? sims.get(simIdx) : new DefaultSimilarity();

            CustomAnalyzers ca = null;
            Analyzer analyzer = null;
            if (fUseSynonyms) {
                ca = CustomAnalyzers.getInstance(wordnetfile);
                analyzer = ca.getSynonymAnalyzer();
            }
            // for testing 
            if (fProcessQuestionsFromFile) {
                processQuestionsDocument p = new processQuestionsDocument(InputFile);
                ArrayList<Question> qList = p.InputToDocumentsList();
                if (fRandomize) {
                    Random randomGenerator = new Random(seed);
                    for (int i = 0; i < 100; i++) {
                        int x = randomGenerator.nextInt(qList.size());
                        Question q = qList.get(x);
                        // System.out.println(q);
                        if (fUseSynonyms) {
                            FilteredSearch(indexDir, q, field, topHits, analyzer, sim);
                        } else {
                            FilteredSearch(indexDir, q, field, topHits, sim);
                        }
                    }
                } else {
                    for (Question q : qList) {
                        // System.out.println(q);
                        if (fUseSynonyms) {
                            FilteredSearch(indexDir, q, field, topHits, analyzer, sim);
                        } else {
                            FilteredSearch(indexDir, q, field, topHits, sim);
                        }
                    }
                }
            } else {
                if (fUseSynonyms) {
                    FilteredSearch(indexDir, new Question("0100", "topic", InputQuery), field, topHits, analyzer, sim);
                } else {
                    FilteredSearch(indexDir, new Question("0100", "topic", InputQuery), field, topHits, sim);
                }
            }
        } catch (IOException | ParseException ex) {

            LOGGER.log(Level.SEVERE, null, ex);
        }
        try {
            if (fTesting) {
                writer.close();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private static void usage(String errMsg) {

        System.err.println(errMsg);
        usage();

    }

    private static void usage() {
        String msg = HcDAChallengeSearcher.class
                .getName()
                + " [-c properties file (full path)] [-q input query (quoted) is required]"
                + "[-h Print this usage information]";
        System.err.println("Usage: " + msg);
    }

    public static void FilteredSearch(String indexDir, Question q, String field, int topHits, Similarity sim)
            throws IOException, ParseException {

        try {
            Directory dir = FSDirectory.open(Paths.get(indexDir));
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher is = new IndexSearcher(reader);
            is.setSimilarity(sim);

            Analyzer analyzer;
            analyzer = new StandardAnalyzer();

            QueryParser parser = new QueryParser(field, analyzer);
            Query query = parser.parse(QueryParser.escape(q.Body));  // tell parser to ignore special characters: escape()

            long start = System.currentTimeMillis();
            TopDocs hits = is.search(query, topHits);
            long end = System.currentTimeMillis();
            System.out.println();
            System.out.println("Found " + hits.totalHits
                    + " document(s) (in " + (end - start)
                    + " milliseconds) that matched query '"
                    + q + "':");

            System.out.println();
            int ii = 0;
            StringBuilder results = new StringBuilder();
            results.append(q.ID);
            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document doc = is.doc(scoreDoc.doc);
                String summary = "(" + ++ii + ") " + "[" + doc.get("docID") + "] " + doc.get(field);
                System.out.println(summary);
                results.append(",");
                results.append(doc.get("docID"));
            }
            // write summary results into a file
            if (HcDAChallengeSearcher.SummaryResultsFile != null) {
                HcDAChallengeSearcher.writer.write(results.toString());
                HcDAChallengeSearcher.writer.write(System.getProperty("line.separator"));
            }
            reader.close();

        } catch (org.apache.lucene.queryparser.classic.ParseException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public static void FilteredSearch(String indexDir, Question q, String field, int topHits, Analyzer analyzer, Similarity sim)
            throws IOException, ParseException {

        try {
            Directory dir = FSDirectory.open(Paths.get(indexDir));
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher is = new IndexSearcher(reader);
            is.setSimilarity(sim);

//            Analyzer analyzer;
//            analyzer = new StandardAnalyzer();
            QueryParser parser = new QueryParser(field, analyzer);
            Query query = parser.parse(QueryParser.escape(q.Body));  // tell parser to ignore special characters: escape()

            long start = System.currentTimeMillis();
            TopDocs hits = is.search(query, topHits);
            long end = System.currentTimeMillis();
            System.out.println();
            System.out.println("Found " + hits.totalHits
                    + " document(s) (in " + (end - start)
                    + " milliseconds) that matched query '"
                    + q + "':");
            System.out.println();
            int ii = 0;
            StringBuilder results = new StringBuilder();
            results.append(q.ID);
            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document doc = is.doc(scoreDoc.doc);
                String summary = "(" + ++ii + ") " + "[" + doc.get("docID") + "] " + doc.get(field);
                System.out.println(summary);
                results.append(",");
                results.append(doc.get("docID"));
            }
            // write summary results into a file
            if (HcDAChallengeSearcher.SummaryResultsFile != null) {
                HcDAChallengeSearcher.writer.write(results.toString());
                HcDAChallengeSearcher.writer.write(System.getProperty("line.separator"));
            }

            reader.close();

        } catch (org.apache.lucene.queryparser.classic.ParseException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

}

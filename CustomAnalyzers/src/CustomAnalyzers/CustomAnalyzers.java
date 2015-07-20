/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package CustomAnalyzers;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.lucene.analysis.util.CharArraySet;

/**
 *
 * @author sargon.hasso
 */
public class CustomAnalyzers {

    private final String SynonymFileName;
    private static CustomAnalyzers instance = null;
    private Analyzer _analyzer;
    
    public static CustomAnalyzers getInstance(String synonymFile) {
        if ( instance == null) {
            instance = new CustomAnalyzers(synonymFile);
        }
        return(instance);
    }

    protected CustomAnalyzers(String synonymFile) {
        this.SynonymFileName = synonymFile;
        this._analyzer = null; 
    }

    public Analyzer getSynonymAnalyzer() throws FileNotFoundException {
        if (this._analyzer != null)
            return(this._analyzer);
        
        Analyzer tempanalyzer = new StandardAnalyzer(CharArraySet.EMPTY_SET);
        WordnetSynonymParser synparser = new WordnetSynonymParser(true, true, tempanalyzer);

        FileReader doctoread = new FileReader(SynonymFileName);
        Analyzer analyzer = null;
        try {
            synparser.parse(doctoread);
            SynonymMap synmap = synparser.build();
            tempanalyzer.close();

            analyzer = new Analyzer() {

                @Override
                protected Analyzer.TokenStreamComponents createComponents(String string) {
                    CharArraySet engstopset = EnglishAnalyzer.getDefaultStopSet();
                    Tokenizer source = new StandardTokenizer();
                    TokenStream filter = new SynonymFilter(source, synmap, true);
                    filter = new StandardFilter(filter);
                    filter = new LowerCaseFilter(filter);
                    filter = new StopFilter(filter, engstopset);
                    return new Analyzer.TokenStreamComponents(source, filter);
                }
            };

        } catch (java.text.ParseException | IOException ex) {
            Logger.getLogger(CustomAnalyzers.class.getName()).log(Level.SEVERE, null, ex);
        }
        this._analyzer = analyzer;
        return (analyzer);
    }
}

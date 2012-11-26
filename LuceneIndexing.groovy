@Grab(group='org.apache.lucene', module='lucene-core', version='4.0.0')
@Grab(group='org.apache.lucene', module='lucene-queryparser', version='4.0.0')
@Grab(group='org.apache.lucene', module='lucene-analyzers-common', version='4.0.0')
@Grab(group='org.apache.lucene', module='lucene-queries', version='4.0.0')

import org.apache.lucene.analysis.*
import org.apache.lucene.analysis.standard.*
import org.apache.lucene.document.*
import org.apache.lucene.index.*
import org.apache.lucene.queryparser.classic.*
import org.apache.lucene.search.*
import org.apache.lucene.store.*
import org.apache.lucene.util.*

// This script takes all the text files in the current directory and indexes each line.
// The script then goes on to search the index for a term which has been passed in as a command line argument.
// Gutenberg.org is a good source of material for testing indexing.

// Main program
assertSearchTermArgumentPassed()
lucene = new LuceneIndex();
deleteOldIndex()
indexTextFilesInCurrentDir()
searchIndexForTerm(this.args[0])

// Helper methods
void indexTextFilesInCurrentDir(){
	def totalNumberOfLines = 0;
	def duration = benchmark({
	new File(".").eachFileMatch(~/.*.txt/) {file ->
		lucene.index(file.text.readLines())
		totalNumberOfLines += countLines(file.text)
	}})
	println "Indexed $totalNumberOfLines lines in ${duration} ms."
}

void searchIndexForTerm(String term) {
	def matches = [];
	def duration = benchmark( { matches = lucene.searchFor(term) } )
	println "Completed search of index in ${duration} ms found ${matches.size()} matches."
	matches.each {println it}
}

int countLines(String text){
	return (text =~ /(?m)$/).size()
}

void deleteOldIndex(){
	new File("index").deleteDir()
}

int benchmark(Closure closure) {
	def start = System.currentTimeMillis()
	closure.call()
	def now = System.currentTimeMillis()
	return now - start
}

void assertSearchTermArgumentPassed(){
	assert args.length == 1 : "Must pass one term to search for i.e. groovy ${this.getClass().getName()}.groovy Monkey"
}

class LuceneIndex {
	def indexName = "index"
	def maxSearchMatches = 10000;
	def indexDirectory = FSDirectory.open(new File(indexName))
	def analyzer = new StandardAnalyzer(Version.LUCENE_40)
	def writerConfiguration = new IndexWriterConfig(Version.LUCENE_40, analyzer)
	
	void index(List<String> texts) {
		def indexWriter = new IndexWriter(indexDirectory, writerConfiguration);
		texts.each {
			Document doc = new Document();
			doc.add(new TextField("content", it, Field.Store.YES))
			indexWriter.addDocument(doc)
		}
		indexWriter.close()
	}
	
	List<String> searchFor(String searchTerm) {
		def indexReader = DirectoryReader.open(indexDirectory);
		def query = new QueryParser(Version.LUCENE_40, "content", analyzer).parse(searchTerm);
		def indexSearcher = new IndexSearcher(indexReader);
		def hits =  indexSearcher.search(query, maxSearchMatches).scoreDocs;
		def matchingStrings = hits.collect{indexSearcher.doc(it.doc).content}
		indexReader.close();
		return matchingStrings
	}
}


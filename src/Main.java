public class Main {
    public static void main(String[] args) {

        // Create an instance if CustomSC4J.
        CustomYT4J sc4J = new CustomYT4J();
        // Start searching with title "Seikin music".
        sc4J.startSearch("Seikin music");

        // Start second search.
        sc4J.continueSearch();

        // Print all title and details in the search result.
        sc4J.getSearchResult().forEach(s -> System.out.println(s.toString()));
    }
}
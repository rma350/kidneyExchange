package protoModeler;

import kepModeler.ModelerInputs;

/** Gross... */
public interface PredicateFactory<V, E> {

  public PredicateBuilder<V, E> createPredicateBuilder(
      ModelerInputs<V, E> modelerInputs);
}

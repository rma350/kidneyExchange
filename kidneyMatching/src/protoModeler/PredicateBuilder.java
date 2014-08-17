package protoModeler;

import java.util.List;

import kepProtos.KepProtos;

import com.google.common.base.Predicate;

public interface PredicateBuilder<V, E> {

  public List<Predicate<E>> makeEdgePredicate(
      KepProtos.EdgePredicate protoEdgePredicate);

}

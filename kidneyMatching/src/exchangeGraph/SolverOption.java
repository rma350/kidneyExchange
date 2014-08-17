package exchangeGraph;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public enum SolverOption {
  edgeMode, cycleMode, subsetMode, cutsetMode, // default

  expandedFormulation, // if not selected, will not create variables for flow in
                       // and flow out, default
  useStrongBranching,

  heuristicCallback, // if selected, will run the heuristic callback up to 5
                     // times, otherwise heuristic callback will not be run

  lazyConstraintCallback, // default
  userCutCallback, // default
  ignoreMaxChainLength,

  silentNoSolutionFound,

  disableFullUserCut, // if selected, no max flow min cut will be performed for
                      // separation at the root node, only O(n^2) time spent on
                      // user cut separation

  disableBoundedChainsAdvancedStart;// if not selected, will produced an
                                    // advanced start for the bounded chain
                                    // length problem by solving the unbounded
                                    // version and truncating the chains

  public static final ImmutableSet<SolverOption> constriantModes = Sets
      .immutableEnumSet(cutsetMode, subsetMode, cycleMode, edgeMode);

  public static ImmutableSet<SolverOption> makeCheckedOptions(
      SolverOption option, SolverOption... solverOptions) {
    EnumSet<SolverOption> ans = EnumSet.of(option, solverOptions);
    Set<SolverOption> selectedConsraintModes = Sets.intersection(
        constriantModes, ans);
    if (selectedConsraintModes.size() == 0) {
      System.err
          .println("Warning, constraint mode was not selected, choosing cutset mode");
      ans.add(cutsetMode);
    } else if (selectedConsraintModes.size() > 1) {
      System.err
          .println("Warning: Options contained more than one constraint mode: "
              + selectedConsraintModes + ", ignoring and using cutset mode");
      ans.removeAll(selectedConsraintModes);
      ans.add(cutsetMode);
    }
    if (!ans.contains(lazyConstraintCallback)) {
      System.err
          .println("Warning: not using lazy constraints may will result in solver not producing a correct solution");
    }
    if (ans.contains(ignoreMaxChainLength)) {
      System.err.println("Warning: max chain length will be ignored.");
    }
    if (ans.contains(heuristicCallback) && ans.contains(edgeMode)) {
      System.err
          .println("Heuristic callback not yet implemented for edge polytope, dropping heuristic");
      ans.remove(heuristicCallback);
    }
    return Sets.immutableEnumSet(ans);
  }

  public static SolverOption getConstraintMode(Set<SolverOption> solverOptions) {
    SetView<SolverOption> selections = Sets.intersection(constriantModes,
        solverOptions);
    if (selections.size() != 1) {
      throw new RuntimeException(
          "Only one constraint mode should be selected but found: "
              + selections);
    }
    return selections.iterator().next();
  }

  public static ImmutableSet<SolverOption> defaultOptions = Sets
      .immutableEnumSet(cutsetMode, expandedFormulation,
          lazyConstraintCallback, userCutCallback, heuristicCallback);

  /**
   * Optimizes the solver options selected for two phase problems where in the
   * first phase, the solution is guarenteed to be a solution to the KEP, and
   * the second phase is simply a truncation based on edge failures. In this
   * case, we know that in phase two, there will be no cycles or chains with
   * legnth above the KEP limit, as the cycles where phase one feasible and the
   * chains are truncated (so are at most as along as a phase one feasible
   * chain).
   * 
   * @param solverOptions
   *          the options passed in to solve the phase one problem
   * @return the options to be used for the phase two problems
   */
  public static ImmutableSet<SolverOption> phaseTwoTrunctationOptions(
      Set<SolverOption> solverOptions) {
    Set<SolverOption> ans = Sets.difference(solverOptions,
        EnumSet.of(lazyConstraintCallback, userCutCallback)).copyInto(
        EnumSet.noneOf(SolverOption.class));
    ans.add(ignoreMaxChainLength);
    return Sets.immutableEnumSet(ans);
  }
}
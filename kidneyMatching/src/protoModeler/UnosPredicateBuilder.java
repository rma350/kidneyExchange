package protoModeler;

import java.util.List;
import java.util.Map;

import kepModeler.ModelerInputs;
import kepProtos.KepProtos;
import kepProtos.KepProtos.DonorPredicate;
import kepProtos.KepProtos.HLA;
import kepProtos.KepProtos.PatientPredicate;
import unosData.UnosDonor;
import unosData.UnosDonorEdge;
import unosData.UnosExchangeUnit;
import unosData.UnosPatient;

import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;

import data.BloodType;
import data.Genotype;
import data.HlaType;

public class UnosPredicateBuilder extends
    BasePredicateBuilder<UnosExchangeUnit, UnosDonorEdge> {

  private UnosHistoricData unosHistoricData;

  private boolean personToNodeInitialized;
  private Map<UnosDonor, UnosExchangeUnit> donorToNode;

  private Map<UnosDonor, Double> donorPoolPowerCache;
  private Map<UnosDonor, Double> donorPoolPraCache;

  private Map<UnosPatient, Double> patientPoolPraCache;

  public UnosPredicateBuilder(
      ModelerInputs<UnosExchangeUnit, UnosDonorEdge> inputs,
      UnosHistoricData unosHistoricData) {
    super(inputs, unosHistoricData);
    donorPoolPowerCache = Maps.newHashMap();
    donorPoolPraCache = Maps.newHashMap();

    patientPoolPraCache = Maps.newHashMap();

    this.unosHistoricData = unosHistoricData;
    personToNodeInitialized = false;
  }

  private void checkPersonToNode() {
    if (!this.personToNodeInitialized) {
      donorToNode = Maps.newHashMap();
      // patientToNode = Maps.newHashMap();
      for (UnosExchangeUnit unit : getInputs().getKepProblemData().getGraph()
          .getVertices()) {
        for (UnosDonor donor : unit.getDonors()) {
          donorToNode.put(donor, unit);
        }
        // if (unit.getPatient() != null) {
        // patientToNode.put(unit.getPatient(), unit);
        // }
      }
      this.personToNodeInitialized = true;
    }
  }

  @Override
  public UnosHistoricData getHistoricData() {
    return this.unosHistoricData;
  }

  @Override
  public Predicate<UnosDonorEdge> checkSameCenter() {
    return new Predicate<UnosDonorEdge>() {
      public boolean apply(UnosDonorEdge edge) {
        UnosExchangeUnit source = getInputs().getKepProblemData().getGraph()
            .getSource(edge);
        UnosExchangeUnit dest = getInputs().getKepProblemData().getGraph()
            .getDest(edge);
        for (UnosDonor donor : source.getDonors()) {
          if (donor.getCenter() == dest.getPatient().getCenter()) {
            return true;
          }
        }
        return false;
      }
    };
  }

  private static ImmutableBiMap<HLA, HlaType> hlaMap = ImmutableBiMap
      .<HLA, HlaType> builder().put(HLA.HLA_A, HlaType.A)
      .put(HLA.HLA_B, HlaType.B).put(HLA.Cw, HlaType.Cw)
      .put(HLA.DP, HlaType.DP).put(HLA.DQ, HlaType.DQ).put(HLA.DR, HlaType.DR)
      .build();

  private static ImmutableBiMap<KepProtos.BloodType, BloodType> bloodTypeMap = ImmutableBiMap
      .<KepProtos.BloodType, BloodType> builder()
      .put(KepProtos.BloodType.A, BloodType.A)
      .put(KepProtos.BloodType.B, BloodType.B)
      .put(KepProtos.BloodType.AB, BloodType.AB)
      .put(KepProtos.BloodType.O, BloodType.O).build();

  @Override
  public Predicate<UnosExchangeUnit> donorPredicate(
      DonorPredicate protoDonorPredicate,
      KepProtos.Relation combinePredicatesForDonor,
      KepProtos.Relation combineDonorsForNode) {
    final Predicate<UnosDonor> singleDonorPredicate = ProtoUtil
        .combinePredicates(unosDonorPredicate(protoDonorPredicate),
            combinePredicatesForDonor);

    if (combineDonorsForNode == KepProtos.Relation.AND) {
      return new Predicate<UnosExchangeUnit>() {
        public boolean apply(UnosExchangeUnit unit) {
          for (UnosDonor donor : unit.getDonors()) {
            if (!singleDonorPredicate.apply(donor)) {
              return false;
            }
          }
          return true;
        }
      };
    } else if (combineDonorsForNode == KepProtos.Relation.OR) {
      return new Predicate<UnosExchangeUnit>() {
        public boolean apply(UnosExchangeUnit unit) {
          for (UnosDonor donor : unit.getDonors()) {
            if (singleDonorPredicate.apply(donor)) {
              return true;
            }
          }
          return false;
        }
      };
    } else {
      throw new RuntimeException(
          "Unexpected relation for combining donors in node: "
              + combineDonorsForNode);
    }
  }

  public ImmutableList<Predicate<UnosDonor>> unosDonorPredicate(
      DonorPredicate protoDonorPredicate) {
    ImmutableList.Builder<Predicate<UnosDonor>> ans = ImmutableList.builder();
    for (KepProtos.HLA hla : protoDonorPredicate.getHomozygousList()) {
      final HlaType hlaType = hlaMap.get(hla);
      ans.add(new Predicate<UnosDonor>() {
        public boolean apply(UnosDonor donor) {
          Genotype genotype = donor.getTissueType().getHlaTypes().get(hlaType);
          return genotype.getAlleleHi() == genotype.getAlleleLo();
        }
      });
    }
    if (protoDonorPredicate.getBloodTypeCount() > 0) {
      final List<BloodType> bloodTypes = convertBlood(protoDonorPredicate
          .getBloodTypeList());
      ans.add(new Predicate<UnosDonor>() {
        public boolean apply(UnosDonor donor) {
          return bloodTypes.contains(donor.getBloodType());
        }
      });
    }
    if (protoDonorPredicate.hasPoolDonorPower()) {
      final Range<Double> range = ProtoUtil.createRange(protoDonorPredicate
          .getPoolDonorPower());
      ans.add(new Predicate<UnosDonor>() {
        public boolean apply(UnosDonor donor) {
          UnosExchangeUnit node = getNodeForDonor(donor);
          double donorPower = cacheComputeDonorPower(donor, node);
          return range.contains(donorPower);
        }
      });
    }
    if (protoDonorPredicate.hasPoolPra()) {
      final Range<Double> range = ProtoUtil.createRange(protoDonorPredicate
          .getPoolPra());
      ans.add(new Predicate<UnosDonor>() {
        public boolean apply(UnosDonor donor) {
          double donorPoolPra = cacheComputeDonorPra(donor);
          return range.contains(donorPoolPra);
        }
      });
    }
    if (protoDonorPredicate.hasHistoricDonorPower()) {
      final Range<Double> range = ProtoUtil.createRange(protoDonorPredicate
          .getHistoricDonorPower());
      ans.add(new Predicate<UnosDonor>() {
        public boolean apply(UnosDonor donor) {
          double donorPower = getHistoricData().historicDonorPower(donor);
          return range.contains(donorPower);
        }
      });
    }
    if (protoDonorPredicate.hasHistoricPra()) {
      final Range<Double> range = ProtoUtil.createRange(protoDonorPredicate
          .getHistoricPra());
      ans.add(new Predicate<UnosDonor>() {
        public boolean apply(UnosDonor donor) {
          double donorPra = getHistoricData().historicDonorPra(donor);
          return range.contains(donorPra);
        }
      });
    }
    return ans.build();
  }

  @Override
  public List<Predicate<UnosExchangeUnit>> patientPredicate(
      PatientPredicate protoPatientPredicate) {
    List<Predicate<UnosExchangeUnit>> ans = Lists.newArrayList();
    for (final Predicate<UnosPatient> patientPredicate : unosPatientPredicate(protoPatientPredicate)) {
      ans.add(new Predicate<UnosExchangeUnit>() {
        public boolean apply(UnosExchangeUnit unit) {
          return patientPredicate.apply(unit.getPatient());
        }
      });
    }
    return ans;
  }

  private static List<BloodType> convertBlood(
      List<KepProtos.BloodType> bloodTypes) {
    return Lists.newArrayList(Lists.transform(bloodTypes,
        Functions.forMap(bloodTypeMap)));
  }

  public ImmutableList<Predicate<UnosPatient>> unosPatientPredicate(
      PatientPredicate protoPatientPredicate) {
    ImmutableList.Builder<Predicate<UnosPatient>> ans = ImmutableList.builder();
    if (protoPatientPredicate.getBloodTypeCount() > 0) {
      final List<BloodType> bloodTypes = convertBlood(protoPatientPredicate
          .getBloodTypeList());
      ans.add(new Predicate<UnosPatient>() {
        public boolean apply(UnosPatient patient) {
          return bloodTypes.contains(patient.getBloodType());
        }
      });
    }
    if (protoPatientPredicate.hasPoolPatientPra()) {
      final Range<Double> range = ProtoUtil.createRange(protoPatientPredicate
          .getPoolPatientPra());
      ans.add(new Predicate<UnosPatient>() {
        public boolean apply(UnosPatient patient) {
          double donorPoolPra = cacheComputePatientPra(patient);
          return range.contains(donorPoolPra);
        }
      });
    }
    if (protoPatientPredicate.hasHistoricPatientPra()) {
      final Range<Double> range = ProtoUtil.createRange(protoPatientPredicate
          .getHistoricPatientPra());
      ans.add(new Predicate<UnosPatient>() {
        public boolean apply(UnosPatient patient) {
          double donorPra = getHistoricData().historicPatientPra(patient);
          return range.contains(donorPra);
        }
      });
    }
    if (protoPatientPredicate.hasPatientAge()) {
      final Range<Double> range = ProtoUtil.createRange(protoPatientPredicate
          .getPatientAge());
      ans.add(new Predicate<UnosPatient>() {
        public boolean apply(UnosPatient patient) {
          return range.contains((double) patient.getAgeYears());
        }
      });
    }
    return ans.build();
  }

  private double cacheComputeDonorPower(UnosDonor donor,
      UnosExchangeUnit donorNode) {
    if (this.donorPoolPowerCache.containsKey(donor)) {
      return donorPoolPowerCache.get(donor);
    } else {
      double ans = ProtoUtil.computeDonorPower(donor, donorNode, getInputs()
          .getKepProblemData());
      donorPoolPowerCache.put(donor, ans);
      return ans;
    }
  }

  private double cacheComputeDonorPra(UnosDonor donor) {
    if (this.donorPoolPraCache.containsKey(donor)) {
      return donorPoolPraCache.get(donor);
    } else {
      double ans = ProtoUtil.computeDonorPra(donor, getInputs()
          .getKepProblemData());
      donorPoolPraCache.put(donor, ans);
      return ans;
    }
  }

  private double cacheComputePatientPra(UnosPatient patient) {
    if (this.patientPoolPraCache.containsKey(patient)) {
      return patientPoolPraCache.get(patient);
    } else {
      double ans = ProtoUtil.computePatientPra(patient, getInputs()
          .getKepProblemData());
      patientPoolPraCache.put(patient, ans);
      return ans;
    }
  }

  private UnosExchangeUnit getNodeForDonor(UnosDonor donor) {
    checkPersonToNode();
    return donorToNode.get(donor);
  }

  // private UnosExchangeUnit getNodeForPatient(UnosPatient patient) {
  // checkPersonToNode();
  // return this.patientToNode.get(patient);
  // }

  public static interface UnosHistoricData extends
      HistoricData<UnosExchangeUnit, UnosDonorEdge> {

    public double historicDonorPower(UnosDonor donor);

    public double historicDonorPra(UnosDonor donor);

    public double historicPatientPra(UnosPatient patient);
  }

  @Override
  public List<Predicate<UnosDonorEdge>> donorEdgePredicate(
      DonorPredicate protoDonorPredicate) {
    List<Predicate<UnosDonorEdge>> ans = Lists.newArrayList();
    for (final Predicate<UnosDonor> donorPredicate : unosDonorPredicate(protoDonorPredicate)) {
      ans.add(new Predicate<UnosDonorEdge>() {
        public boolean apply(UnosDonorEdge edge) {
          return donorPredicate.apply(edge.getDonor());
        }
      });
    }
    return ans;
  }

}

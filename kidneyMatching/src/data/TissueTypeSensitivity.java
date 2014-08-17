package data;

import java.util.EnumMap;

public class TissueTypeSensitivity {

  private EnumMap<HlaType, int[]> antibodies;
  // is true if the person avoids the entry
  private EnumMap<SpecialHla, Boolean> avoidsSpecialAntibodies;

  public TissueTypeSensitivity() {
    super();
    antibodies = new EnumMap<HlaType, int[]>(HlaType.class);
    avoidsSpecialAntibodies = new EnumMap<SpecialHla, Boolean>(SpecialHla.class);
  }

  public EnumMap<HlaType, int[]> getAntibodies() {
    return antibodies;
  }

  public EnumMap<SpecialHla, Boolean> getAvoidsSpecialAntibodies() {
    return avoidsSpecialAntibodies;
  }

  public boolean isCompatible(TissueType tissueType) {
    for (HlaType hlaType : HlaType.values()) {
      if (tissueType.getHlaTypes().get(hlaType) != null
          && contains(this.antibodies.get(hlaType), tissueType.getHlaTypes()
              .get(hlaType))) {
        return false;
      }
    }
    for (SpecialHla specialHlaType : SpecialHla.values()) {
      if (tissueType.getSpecialHla().get(specialHlaType).booleanValue()
          && this.avoidsSpecialAntibodies.get(specialHlaType).booleanValue()) {
        return false;
      }
    }
    return true;

  }

  private static boolean contains(int[] a, Genotype g) {
    for (int i = 0; i < a.length; i++) {
      if (a[i] == g.getAlleleLo() || a[i] == g.getAlleleHi()) {
        return true;
      }
    }
    return false;
  }

}

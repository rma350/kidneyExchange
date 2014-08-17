package unosData;

public class UnosDonorEdge {
  private UnosDonor donor;

  public UnosDonorEdge(UnosDonor donor) {
    this.donor = donor;
  }

  public UnosDonor getDonor() {
    return this.donor;
  }

  @Override
  public String toString() {
    return "e_" + this.donor.toString();
  }
}

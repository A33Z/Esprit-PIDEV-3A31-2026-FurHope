package com.esprit.animal.entities;

public class AnimalStatistics {

    private int totalAnimals;
    private int availableAnimals;
    private int adoptedAnimals;
    private int totalRequests;
    private int pendingRequests;
    private int approvedRequests;
    private int rejectedRequests;
    private String mostCommonSpecies;

    public int getTotalAnimals() {
        return totalAnimals;
    }

    public void setTotalAnimals(int totalAnimals) {
        this.totalAnimals = totalAnimals;
    }

    public int getAvailableAnimals() {
        return availableAnimals;
    }

    public void setAvailableAnimals(int availableAnimals) {
        this.availableAnimals = availableAnimals;
    }

    public int getAdoptedAnimals() {
        return adoptedAnimals;
    }

    public void setAdoptedAnimals(int adoptedAnimals) {
        this.adoptedAnimals = adoptedAnimals;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getPendingRequests() {
        return pendingRequests;
    }

    public void setPendingRequests(int pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    public int getApprovedRequests() {
        return approvedRequests;
    }

    public void setApprovedRequests(int approvedRequests) {
        this.approvedRequests = approvedRequests;
    }

    public int getRejectedRequests() {
        return rejectedRequests;
    }

    public void setRejectedRequests(int rejectedRequests) {
        this.rejectedRequests = rejectedRequests;
    }

    public String getMostCommonSpecies() {
        return mostCommonSpecies;
    }

    public void setMostCommonSpecies(String mostCommonSpecies) {
        this.mostCommonSpecies = mostCommonSpecies;
    }
}


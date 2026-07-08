package net.aspw.client.utils.pathfinder;

import java.util.ArrayList;

public class PathHub {
    private Vec3 loc;
    private ArrayList<Vec3> pathway;
    private double sqDist;
    private double currentCost;
    private double maxCost;

    public PathHub(final Vec3 loc, final PathHub parentPathHub, final ArrayList<Vec3> pathway,
                   final double sqDist, final double currentCost, final double maxCost) {
        this.loc = loc;
        this.pathway = pathway;
        this.sqDist = sqDist;
        this.currentCost = currentCost;
        this.maxCost = maxCost;
    }

    public Vec3 getLoc() {
        return this.loc;
    }

    public ArrayList<Vec3> getPathway() {
        return this.pathway;
    }

    public double getSqDist() {
        return this.sqDist;
    }

    public double getCurrentCost() {
        return this.currentCost;
    }

    public void setLoc(final Vec3 loc) {
        this.loc = loc;
    }

    public void setParentPathHub(final PathHub parentPathHub) {
    }

    public void setPathway(final ArrayList<Vec3> pathway) {
        this.pathway = pathway;
    }

    public void setSqDist(final double sqDist) {
        this.sqDist = sqDist;
    }

    public void setCurrentCost(final double currentCost) {
        this.currentCost = currentCost;
    }

    public double getMaxCost() {
        return this.maxCost;
    }

    public void setMaxCost(final double maxCost) {
        this.maxCost = maxCost;
    }
}

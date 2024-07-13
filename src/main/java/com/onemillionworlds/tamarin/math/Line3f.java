
package com.onemillionworlds.tamarin.math;


import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import java.util.Objects;
import java.util.Optional;

public class Line3f{
    public final Vector3f start;
    public final Vector3f end;
    private Vector3f centre;
    private Vector3f startToEndVector;
    private Vector3f startToEndVector_unit;
    private final float length;
    
    public Line3f(Vector3f start, Vector3f end) {
        this.start = start;
        this.end = end;
        length=getStartToEndVector().length();
    }

    @Override
    public boolean equals(Object other){
        if (other instanceof Line3f){
            return ((Line3f) other).start.equals(start) && ((Line3f) other).end.equals(end);
        }else{
            return false;
        }
    }


    @Override
    public String toString(){
        return start.toString() + " to " + end.toString();
    }
    
    public Vector3f getCentrePoint(){
        if (centre==null){
            centre=new Vector3f(0.5f*(start.x+end.x),0.5f*(start.y+end.y),0.5f*(start.z+end.z));
        }
        return centre;
    }
    
    public Vector3f getStartToEndVector(){
        if (startToEndVector==null){
            startToEndVector=end.subtract(start);
        }
        return startToEndVector;
    }
    public Vector3f getStartToEndVector_unit(){
        if (startToEndVector_unit==null){
            startToEndVector_unit=getStartToEndVector().normalize();
        }
        return startToEndVector_unit;
    }
    public float getLength(){
        
        return length;
    }
    
    public Vector3f[] findPointsOfClosestApproach(Line3f otherLine){
        //see http://geomalgorithms.com/a07-_distance.html Distance between Lines section
        
        //this returns the points on both lines at which the lines are at their
        //closest. If the lines are parallel it returns the mid points of 
        //the two lines (not ideal behaviour but probably fine)

        Vector3f line1Vector=getStartToEndVector();

        Vector3f line2Vector=otherLine.getStartToEndVector();
        
        Vector3f w0= start.subtract(otherLine.start);
        
        float a=line1Vector.dot(line1Vector);
        float b=line1Vector.dot(line2Vector);
        float c=line2Vector.dot(line2Vector);
        float d=line1Vector.dot(w0);
        float e=line2Vector.dot(w0);
        
        if (a*c-b*b==0){
            //parallel lines, o noes!
            //for the time could return the mid points
            //of the lines (although this is wrong):
                return new Vector3f[]{this.getCentrePoint(),otherLine.getCentrePoint()};

            //throw new RuntimeException("Parallel lines are not yet properly handled");
            
            
        }else{
            float proportionAlongLine1=(b*e-c*d)/(a*c-b*b);
            float proportionAlongLine2=(a*e-b*d)/(a*c-b*b); 
            
            Vector3f line1Point=this.getPointAProportionAlongLine3f(proportionAlongLine1);
            Vector3f line2Point=otherLine.getPointAProportionAlongLine3f(proportionAlongLine2);
            
            return new Vector3f[]{line1Point,line2Point};
 
        }
    }

    public Vector3f findCentreOfClosedApproach(Line3f otherLine){
        Vector3f[] closestPoints=findPointsOfClosestApproach(otherLine);
        return closestPoints[0].add(closestPoints[1]).mult(0.5f);
    }
    
    public float findDistanceSquaredOfClosedApproach(Line3f otherLine) {
        Vector3f[] closestPoints=findPointsOfClosestApproach(otherLine);
        return closestPoints[0].distanceSquared(closestPoints[1]);
    }

    public float findDistanceLineToPoint(Vector3f otherPoint){
        return FastMath.sqrt(findDistanceSquaredOfClosestApproach(otherPoint));
    }

    public float findDistanceSquaredOfClosestApproach(Vector3f otherPoint){
        Vector3f closedPoint=findPointOfClosedApproach(otherPoint);
        return closedPoint.distanceSquared(otherPoint);
    }
    
    public Vector3f findPointOfClosedApproach(Vector3f otherPoint){
       
        float projection=getProjection(otherPoint);
        
        if (projection<0){
            return start;
        }else if(projection>getLength()){
            return end;
        }else{
            return start.add(getStartToEndVector_unit().mult(projection));
        }
    }
    
    /**
     * Will project the point onto the line (discarding and distance away from the line
     * and will then find how far along the line that projection is, zero for at the start
     * and 1 for at the end.
     * 
     * It CAN go beyond the region of the line
     */
    public float findDistanceAlongLine3f(Vector3f otherPoint){
        return getProjection(otherPoint);
        
    }
     /**
     * Will project the point onto the line (discarding and distance away from the line
     * and will then find how far along the line that projection is, zero for at the start
     * and 1 for at the end.
     * 
     * It CAN go beyond the region of the line, use Uts.clamp(x, 0, 1) to clamp if needed
     */
    public float findFractionalDistanceAlongLine3f(Vector3f otherPoint){
        return findDistanceAlongLine3f(otherPoint)/this.length;
    }
    
    private Vector3f getPointAProportionAlongLine3f(float proportion){
        if (proportion<0){
            return new Vector3f(start);
        }else if (proportion>1){
            return new Vector3f(end);
        }else{
            return start.add(getStartToEndVector().mult(proportion));
        }
    }

    public float getProjection(Vector3f otherPoint){
        Vector3f pointLocalFrame = otherPoint.subtract(start);
        Vector3f lineDirection=getStartToEndVector_unit();
        
        return pointLocalFrame.dot(lineDirection);
    }

    public Vector3f getPointADistanceAlongLine3f(float lengthAlongLine, boolean allowToExtendBeyondLine) {
        if (!allowToExtendBeyondLine && lengthAlongLine<0){
            return start;
        }else if(!allowToExtendBeyondLine && lengthAlongLine>getLength()){
            return end;
        }else{
            return start.add(getStartToEndVector_unit().mult(lengthAlongLine));
        }
    }

    /**
     * this is purely a measure of alignment, the lines physical position is irrelenant to this
     * @param vector 
     */
    public float getProjectionOfVector(Vector3f vector) {
        return this.getStartToEndVector_unit().dot(vector);
    }


    /**
     * Shifts the line by subtacting vector3dJME from both the origin and end
     * @param vector3dJME
     *
     */
    public Line3f subtract(Vector3f vector3dJME) {
        return new Line3f(this.start.subtract(vector3dJME), this.end.subtract(vector3dJME));
    }


    public float findDistanceFromPointOfCloesstApproach(Vector3f worldPosition) {
        return this.findPointOfClosedApproach(worldPosition).distance(worldPosition);
    }

    /**
     * Clips the line down so it only exists inside the box, If no such line exists then an "empty" is returned
     * @param min
     * @param max
     *
     */
    public Optional<Line3f> clipToBeWithinRectangle(Vector3f min, Vector3f max){

        //t is the paramater of the parameterised line, its travel from 0 to 1 builds the line
        Vector3f deltas =end.subtract(start);

        //for example x = x0 + t * dx (dx is deltas.x)

        float currentMinT=0;
        float currentMaxT=1;

        for (int coord=0; coord<3; coord++){

            if (deltas.get(coord) == 0){
                //if the line is parrallel to the window it either completely eliminates the line or has no effect

                double coordinateOnWindow=start.get(coord); //(which equals end.get(coord);

                if(!(min.get(coord) <= coordinateOnWindow) || !(coordinateOnWindow <= max.get(coord))){
                    return Optional.empty(); //its completely clipped away
                }

            }else{
                double tAtStart = (min.get(coord)-start.get(coord))/deltas.get(coord);
                double tAtEnd = (max.get(coord)-start.get(coord))/deltas.get(coord);

                if (tAtStart>tAtEnd){
                    //flip them as the box is the opposite way round to the line
                    double temp=tAtStart;
                    tAtStart=tAtEnd;
                    tAtEnd=temp;
                }

                currentMinT = (float)Math.max(currentMinT,tAtStart);
                currentMaxT = (float)Math.min(currentMaxT,tAtEnd);
            }
        }

        if (currentMaxT<=currentMinT){
            //completely clipped away
            return Optional.empty();
        }else{
            return Optional.of(new Line3f(
                    start.add(deltas.mult(currentMinT)),
                    start.add(deltas.mult(currentMaxT))
            ));
        }
    }

    @Override
    public int hashCode(){
        return Objects.hash(start, end);
    }
}

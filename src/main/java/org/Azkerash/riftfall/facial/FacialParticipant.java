package org.Azkerash.riftfall.facial;

/** Integration point: own a controller and synchronize its packed frame using your entity's data accessor. */
public interface FacialParticipant {
    FacialController facialController();
    FaceFrame facialFrame();
}

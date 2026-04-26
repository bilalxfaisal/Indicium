//package com.indicium.models;
//
//import com.indicium.repository.CaseRepository;
//import com.indicium.services.AccessManager;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//
//
//public class Case {
//    final private int caseID;
//    private String title;
//    private LocalDateTime incidentDate;
//    private CaseStatus status;
//
//    // Evidence List
//    private ArrayList<Evidence> evidenceList;
//    private ArrayList<TimeLineEvent> timeLineEventsList;
//
//
//    public int getCaseID() {return caseID;}
//    //public void setCaseID(int caseID) {this.caseID = caseID;}
//
//    public String getTitle() {return title;}
//    public void setTitle(String title) {this.title = title;}
//
//    public LocalDateTime getIncidentDate() {return incidentDate;}
//    public void setIncidentDate(LocalDateTime incidentDate) {this.incidentDate = incidentDate;}
//
//    public CaseStatus getStatus() {return status;}
//    public void setStatus(CaseStatus status) {this.status = status;}
//
//
//    public Case(int caseID, String title, LocalDateTime incidentDate, CaseStatus status) {
//        this.caseID = caseID;
//        this.title = title;
//        this.incidentDate = incidentDate;
//        this.status = status;
//        this.evidenceList = new ArrayList<>();
//        this.timeLineEventsList = new ArrayList<>();
//    }
//
//    public Case(int caseID, String title, LocalDateTime incidentDate) {
//        // much shorter this way ngl
//        this(caseID, title, incidentDate, CaseStatus.OPEN);
//    }
//
//    public Case(String title, LocalDateTime incidentDate, CaseStatus status) {
//        this.caseID = generateUniqueID();
//        this.title = title;
//        this.incidentDate = incidentDate;
//        this.status = status;
//    }
//
//    public Case(String title, LocalDateTime incidentDate) {
//        this(title, incidentDate, CaseStatus.OPEN);
//    }
//
//    public void addEvidence(Evidence evidence) {
//        if (this.evidenceList.contains(evidence) || evidence == null) return;
//        this.evidenceList.add(evidence);
//    }
//    // returns a copy of the evidence list to prevent external modification
//    public ArrayList<Evidence> getEvidenceList() { return new ArrayList<>(this.evidenceList); }
//
//    public String getDetails() {
//        return String.format("Case ID: %d\nTitle: %s\nIncident Date: %s\nStatus: %s\nEvidence Count: %d",
//                caseID, title, incidentDate.toString(), status.toString(), evidenceList.size());
//    }
//
//    // returns a copy to prevent external modification
//    public ArrayList<TimeLineEvent> getTimeLineEvents() {
//        return new ArrayList<>(this.timeLineEventsList);
//    }
//
//    public boolean verifyStandardViewingPrivileges(int investigatorID){
//       if (AccessManager.isLockDownActive()){
//           System.out.println("[SECURITY] Access Denied. LockDown is Active.");
//           return false;
//       }
//
//        // if case is archived, standard investigators cannot view it
//        if(this.status == CaseStatus.ARCHIVED){
//            System.out.println("[SECURITY] Access Denied. Case is Archived.");
//            return false;
//        }
//
//       return CaseRepository.isUserAssignedToCase(investigatorID, this.caseID);
//    }
//
//    public Case gatherRelevantData(){
//        System.out.println("[INFO] Gathering relevant data for case ID: " + this.caseID);
//        return this; // use yourself as needed
//    }
//
//    // returns true if duplicate found, false otherwise
//    public boolean checkDuplicate(String fileHash){
//        for (Evidence evidence : this.evidenceList){
//            if (evidence.getDigitalFingerprint().equals(fileHash)) {
//                System.out.println("[INFO] Duplicate evidence detected with hash: " + fileHash);
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private int generateUniqueID(){
//        return LocalDateTime.now().hashCode();
//    }
//}
//
//

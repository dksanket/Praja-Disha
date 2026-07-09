package gov.prajadisha.backend.config;

import gov.prajadisha.backend.citizen.model.CitizenProfile;
import gov.prajadisha.backend.citizen.model.PointActivity;
import gov.prajadisha.backend.citizen.repository.CitizenProfileRepository;
import gov.prajadisha.backend.citizen.repository.PointActivityRepository;
import gov.prajadisha.backend.common.GeoPoint;
import gov.prajadisha.backend.common.GeoPolygon;
import gov.prajadisha.backend.org.model.Department;
import gov.prajadisha.backend.org.model.Officer;
import gov.prajadisha.backend.org.model.Organization;
import gov.prajadisha.backend.org.model.OrganizationConfig;
import gov.prajadisha.backend.org.repository.DepartmentRepository;
import gov.prajadisha.backend.org.repository.OfficerRepository;
import gov.prajadisha.backend.org.repository.OrganizationConfigRepository;
import gov.prajadisha.backend.org.repository.OrganizationRepository;
import gov.prajadisha.backend.task.model.DetailedActivity;
import gov.prajadisha.backend.task.model.Task;
import gov.prajadisha.backend.task.repository.TaskRepository;
import gov.prajadisha.backend.task.repository.TaskAssignmentRepository;
import gov.prajadisha.backend.org.repository.ReportingHierarchyRepository;
import gov.prajadisha.backend.citizen.repository.TransitPassRepository;
import gov.prajadisha.backend.ai.repository.AiChatMessageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds New Delhi MP Office details, configuration, 20 departments (5 root and 15 sub-departments),
 * officers, a citizen, and realistic tasks. All descriptions are programmatically guaranteed to be
 * at least 500 words long to facilitate rich AI triage, and all IDs are generated dynamically by MongoDB.
 */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final OrganizationRepository organizations;
    private final OrganizationConfigRepository orgConfigs;
    private final DepartmentRepository departments;
    private final OfficerRepository officers;
    private final CitizenProfileRepository citizens;
    private final PointActivityRepository activities;
    private final TaskRepository tasks;
    private final TaskAssignmentRepository taskAssignments;
    private final ReportingHierarchyRepository reportingHierarchies;
    private final TransitPassRepository transitPasses;
    private final AiChatMessageRepository aiChatMessages;

    // --- 500+ Word Descriptions defined at Class Level to avoid Method Bytecode Size Limits ---

    private static final String ORG_DESC = """
The Member of Parliament (MP) Office of New Delhi serves as the primary municipal, legislative, and developmental representative headquarters for the New Delhi Lok Sabha Constituency. Under the direct leadership of the elected Member of Parliament, this office coordinates all regional governance initiatives, local area development funds, and public welfare programs across the national capital’s most critical districts. The constituency includes prominent administrative, commercial, and residential sectors such as Connaught Place, Chanakyapuri, Karol Bagh, Greater Kailash, Delhi Cantonment, and R.K. Puram. A central pillar of the office's mission is the execution and oversight of projects funded by the Member of Parliament Local Area Development Scheme (MPLADS). These funds are strategically allocated to build, upgrade, and maintain essential community assets including public schools, municipal libraries, dispensaries, community centers, and recreational parks. Beyond fund management, the MP Office functions as a vital liaison and coordination hub connecting local citizens with diverse governing bodies. In Delhi's complex multi-layered administrative landscape, the office acts as a bridge to the Municipal Corporation of Delhi (MCD) for sanitation and solid waste management; the New Delhi Municipal Council (NDMC) for civic services in Lutyens' Delhi; the Delhi Development Authority (DDA) for land allocation and housing; the Delhi Police for civic safety and traffic management; and utility companies like Delhi Jal Board (DJB), BSES Yamuna Power, and BSES Rajdhani Power for water and electricity distribution. The office is dedicated to resolving public grievances through a structured intake process that handles complaints related to infrastructure failures, garbage piles, sewage blockages, electrical outages, streetlighting, water scarcity, and safety concerns. Citizens can register their issues via digital platforms, voice notes, or in-person visits to the constituency office. The MP Office conducts regular public hearings, known as Jan Sabhas, allowing residents, resident welfare associations (RWAs), and merchant associations to directly voice their concerns. These interactions help the office identify development priorities and formulate localized solutions. Additionally, the office serves as an information and registration node for central government social welfare schemes. It assists eligible citizens—especially senior citizens, women, and economically weaker sections—in registering for pensions, health cards under Ayushman Bharat, livelihood support programs, and direct benefit transfer (DBT) schemes. Through modern governance practices, data-driven ticket routing, and AI-enabled triage, the MP Office of New Delhi ensures that every grievance is accurately classified, assigned to the appropriate department, and tracked until resolution, upholding the highest standards of democratic accountability and public service. The office also works closely with local non-governmental organizations, environmental groups, and urban planners to introduce sustainable development models, such as rainwater harvesting systems, solar-powered street lighting grids, and waste-to-energy projects in different wards. By organizing community workshops on digital literacy, health camps, and vocational training sessions, the office strives to empower marginalized communities and promote overall social development. Under its comprehensive master plan, the MP Office aims to convert New Delhi into a world-class smart constituency while preserving its unique historical heritage and cultural identity. The office's administrative staff is trained to handle complex coordination tasks, keeping tracking of inter-departmental projects, preparing detailed progress reports, and maintaining transparent communication with the public. Through regular press briefs and social media updates, the office keeps the citizens informed of all developmental milestones, budget allocations, and policy initiatives. By fostering a collaborative environment where citizens and administrators work hand-in-hand, the MP Office of New Delhi continues to pave the way for progressive, inclusive, and responsive civic governance, ensuring that the voice of every resident is heard and addressed at the highest legislative levels.
""";

    private static final String DPT_MPLADS_DESC = """
The MPLADS and Infrastructure Development Department is the primary administrative unit responsible for the strategic planning, fund management, execution, and quality control of developmental projects funded by the Member of Parliament Local Area Development Scheme (MPLADS) across the New Delhi Lok Sabha constituency. The department's main objective is to identify, prioritize, and construct durable public assets that address localized community needs, such as community halls, open-air gymnasiums, public libraries, primary healthcare centers, and educational facilities. It acts as the central coordinating authority for evaluating infrastructure proposals submitted by resident welfare associations (RWAs), local councilors, non-governmental organizations, and individual citizens. The department oversees the entire lifecycle of capital projects, starting from feasibility analysis, site selection, and detailed budget preparation to the preparation of tender documents, vendor selection, and project monitoring. To ensure seamless execution, the department maintains active liaison with executing agencies such as the Central Public Works Department (CPWD), the Delhi Development Authority (DDA), the Municipal Corporation of Delhi (MCD), the New Delhi Municipal Council (NDMC), and various public works departments. It conducts structural audits of public buildings, monitors the quality of materials used in construction, and enforces strict compliance with national building codes and safety guidelines. The department's scope of work also includes checking structural defects, reviewing construction delays, managing funding disbursements, and preparing audit reports for parliamentary review. It handles public inquiries regarding fund utilization, project timelines, and contractor performance. In addition, the department coordinates with urban planning experts to integrate sustainable design practices, such as rainwater harvesting systems and solar-powered facilities, into new construction projects. It is dedicated to ensuring that all public infrastructure is inclusive, providing ramp installations and accessibility features for disabled and senior citizens. By maintaining a transparent, public-facing database of all ongoing and completed projects, the department ensures high levels of public accountability and community participation in urban development. It actively tracks project delays, issues completion certificates, and facilitates physical site inspections to guarantee that every rupee allocated delivers real value. The department serves as the foundation for the constituency's long-term physical growth, striving to deliver assets that foster social unity, education, health, and wellness for millions of residents in New Delhi.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_PW_AMENITIES_DESC = """
The Public Works and Civic Amenities Department, operating under the MPLADS and Infrastructure division, is dedicated to the enhancement, maintenance, and development of public spaces, parks, recreational areas, and civic amenities throughout the New Delhi constituency. The department's primary mission is to provide clean, safe, and accessible outdoor spaces and public facilities for all residents. Its operational scope includes the development of community parks, children's play areas, open-air gymnasiums, public toilet blocks, passenger shelters, senior citizen seating areas, and walking tracks. The department is responsible for horticultural development, regular maintenance of green covers, tree plantation drives, and landscaping of public roundabouts. It coordinates directly with the Delhi Development Authority (DDA), the Municipal Corporation of Delhi (MCD), and the New Delhi Municipal Council (NDMC) to manage land clearances and resolve jurisdictional overlaps for public amenities. Key tasks include installing benches, repairing walking tracks, setting up open gym equipments, maintaining public drinking water kiosks, fixing broken fencing in parks, and ensuring proper lighting inside public parks for safety. The department handles complaints regarding damaged playground equipment, overgrown grass, dry trees, lack of water supply in public toilets, broken park gates, and general upkeep of civic spaces. Operational procedures involve regular weekly inspections of public parks, prompt response to citizen feedback, and working with local Resident Welfare Associations (RWAs) to maintain public cleanliness. The department also implements eco-friendly initiatives like setting up compost pits for dry leaves, installing drip irrigation systems, and utilizing recycled sewage water for maintaining lawns. By creating well-maintained, green, and vibrant civic spaces, the department strives to improve public health, foster community interaction, and elevate the aesthetic standards of the New Delhi constituency. It coordinates seasonal flower shows, manages urban forestry projects, and updates public recreational machinery, ensuring that citizens have modern and refreshing green retreats amidst the bustling urban environment.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_EDU_HEALTH_DESC = """
The Education and Healthcare Infrastructure Development Department is a specialized sub-department focused on upgrading and expanding the educational and healthcare facilities within the New Delhi constituency using MPLADS and local development funds. The department recognizes that high-quality schools and accessible healthcare are fundamental rights, and it works tirelessly to improve the structural conditions of these institutions. The department's responsibilities include construction of new classrooms, laboratory facilities, school library buildings, clean drinking water filtration systems, separate toilet blocks for girls and boys in government schools, and structural renovations of old school buildings. In the healthcare sector, the department manages the construction and upgrading of primary health centers, dispensaries, community health clinics, and mohalla clinics. It coordinates the procurement of basic medical equipment, upgrades patient waiting halls, and builds ramps and elevators for disabled access in clinics. The department works in collaboration with the Directorate of Education, the Department of Health and Family Welfare of the Delhi Government, MCD, and NDMC. Typical grievances handled by this department include lack of basic amenities in schools (such as broken benches, non-functional toilets, leaky roofs, or lack of clean water), structural damage to local health clinics, lack of medical equipment or beds in dispensaries, and requests for upgrading local public libraries with computers and digital books. The department conducts regular safety audits of school buildings, coordinates fire safety clearances, and monitors construction quality to ensure a safe learning and healing environment. By prioritizing the infrastructure needs of schools and health centers, the department aims to provide the best possible services to students, patients, and the community at large, ensuring inclusive growth and development. It actively supports digital education initiatives by funding computer labs and smart classrooms in municipal schools, while also building modern wellness centers to deliver basic healthcare services close to citizens' doorsteps.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_URBAN_HERITAGE_DESC = """
The Urban Renewal and Heritage Preservation Department is responsible for conserving the rich historical heritage and revitalizing the urban landscape of the New Delhi constituency. New Delhi is home to numerous historical monuments, colonial heritage buildings, and historic commercial markets like Connaught Place and Karol Bagh. The department's mission is to balance rapid modernization with the preservation of the area's unique historical and cultural identity. Its scope of work includes the conservation and beautification of heritage structures, implementing uniform facade design policies in historic markets, organizing streetscape projects, creating pedestrian-friendly zones, and installing heritage-themed signage and streetlighting. The department collaborates with the Archaeological Survey of India (ASI), the Delhi Urban Art Commission (DUAC), INTACH, DDA, and local municipal corporations. It handles complaints and projects related to encroachment near heritage monuments, defacement of historical buildings, poorly maintained tourist amenities, unorganized signage in commercial areas, and damaged pedestrian pavements. The department also undertakes urban renewal projects to restore old, congested markets, upgrade drainage and utility wiring systems in historic areas, and install public art installations that reflect the cultural history of New Delhi. It coordinates regular inspections to prevent unauthorized construction near protected monuments and works with local business associations to ensure clean and vibrant commercial spaces. By implementing heritage conservation plans and modern urban design principles, the department aims to enhance the tourist appeal of New Delhi, boost local business, and preserve the city's historical legacy for future generations. The department also organizes cultural walks, funds tourist information kiosks, and maintains lighting grids for monument illumination, fostering a deep appreciation for the national capital's unique history and architectural landmarks.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_SAN_GRIEV_DESC = """
The Public Grievances, Sanitation and Waste Management Department is the cornerstone of the MP Office's citizen-centric services, responsible for managing the intake, tracking, and resolution of all civic complaints, with a primary focus on public hygiene, sanitation, and municipal services. The department acts as the primary contact point for citizens facing issues in their day-to-day lives. It operates a centralized grievance redressal system that monitors complaint registration, coordinates with municipal agencies, and ensures timely resolution of public issues. The department's main objective is to establish a clean, hygienic, and livable environment throughout the New Delhi constituency. It coordinates closely with the Municipal Corporation of Delhi (MCD) and the New Delhi Municipal Council (NDMC) to monitor the delivery of municipal services. Core responsibilities include managing public complaints related to garbage clearance, drainage cleaning, public toilet hygiene, street sweeping, pest control, and local waste disposal. The department handles a high volume of daily complaints and uses data analytics to identify grievance hotspots and bottleneck areas in the constituency. It monitors the performance of local sanitary inspectors and waste collection agencies, enforcing service level agreements and accountability. The department also coordinates regular sanitation drives in residential areas, commercial markets, and slums. It works with Resident Welfare Associations (RWAs) to raise awareness about source segregation of waste and organic composting. By ensuring a prompt, transparent, and effective grievance redressal mechanism, the department aims to elevate the sanitation and hygiene standards of New Delhi, creating a healthy and pollution-free living environment for all constituents. The department compiles weekly grievance resolution reports, implements citizens' charter mandates, and holds direct review meetings with municipal commissioners to expedite pending public complaints.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_WASTE_MGMT_DESC = """
The Municipal Cleanliness and Solid Waste Management Department is a specialized unit dedicated to managing garbage collection, solid waste disposal, recycling initiatives, and street cleanliness within the New Delhi constituency. The department's main goal is to ensure a zero-garbage public space by implementing efficient waste management systems. Its operational scope includes overseeing door-to-door garbage collection, maintaining municipal garbage bins (dhalaos), organizing street sweeping schedules, managing commercial waste disposal in busy markets, and coordinating plastic waste management drives. The department works in constant coordination with MCD, NDMC, and private concessionaires responsible for waste management. It handles citizen complaints regarding accumulated garbage piles on roadsides, irregular door-to-door waste collection, overflowing municipal dustbins, dirty commercial markets, lack of dustbins in public areas, and illegal dumping of construction waste. The department's standard operating procedures include monitoring waste collection vehicles via GPS, conducting daily morning inspections of main markets and roads, and coordinating special cleanliness drives during festivals and public events. The department also promotes sustainable waste management practices, such as source segregation (wet, dry, and sanitary waste), setting up community compost units, and establishing decentralized recycling centers. By enforcing municipal sanitation bylaws and ensuring timely waste collection and disposal, the department strives to eliminate open garbage dumps, reduce landfill waste, and maintain the aesthetic cleanliness of the New Delhi constituency. It regularly monitors municipal composting plants, manages e-waste drop-off kiosks, and conducts mass awareness programs to educate residents on green living.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_DRAINAGE_DESC = """
The Drainage, Sewerage and Water Logging Control Department is responsible for maintaining the drainage and sewerage networks, preventing waterlogging, and managing stormwater runoff in the New Delhi constituency. Due to the high density of population and heavy monsoon rains, maintaining a functional drainage system is critical to prevent urban flooding and public health hazards. The department's core duties include coordinates monsoon desilting of stormwater drains, repairing broken sewer pipelines, replacing damaged manhole covers, cleaning blocked internal drains, and managing water logging pump stations at low-lying areas. The department works closely with the Delhi Jal Board (DJB), the Public Works Department (PWD), MCD, and NDMC. It handles citizen grievances regarding blocked sewer lines, sewage water backflowing into houses, overflowing manholes on roads, heavy waterlogging during rains, missing gully grates, and damaged stormwater drains. The department's operational strategy involves pre-monsoon desilting audits, deploying quick-response teams with sewer cleaning machines during heavy rainfall, and installing water level sensors at flood-prone underpasses. It also focuses on upgrading old drainage networks in historical areas and implementing rainwater harvesting structures to recharge groundwater levels. By ensuring free-flowing sewers and drains, the department aims to prevent waterborne diseases, eliminate traffic disruptions caused by flooding, and protect civic infrastructure from water damage. It manages emergency dewatering pumps, schedules regular cleaning of catch basins, and coordinates structural upgrades for sewerage trunk lines to support the city's growth.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_ENV_POLLUTION_DESC = """
The Environmental Protection and Pollution Control Department is responsible for monitoring and mitigating environmental pollution, preserving local biodiversity, and enhancing the green cover of the New Delhi constituency. The national capital faces severe environmental challenges, particularly air pollution during winter, making this department's role highly critical. Its operational scope includes monitoring ambient air quality, enforcing dust control measures at construction sites, preventing illegal burning of dry leaves and garbage, implementing noise pollution regulations, and protecting local water bodies from pollution. The department coordinates with the Delhi Pollution Control Committee (DPCC), the Central Board (CPCB), the Forest Department, and municipal bodies. It handles citizen complaints regarding open burning of waste, heavy dust emissions from construction projects, noise from illegal commercial activities or loudspeakers, pollution in local ponds or the Yamuna riverfront, and unauthorized felling of trees. The department's preventive measures include deploying water sprinkler tankers and anti-smog guns on dusty roads, organizing massive tree plantation drives (Van Mahotsav) to increase the green canopy, installing air purifiers at public hotspots, and promoting the use of public transport and electric vehicles. The department also works with Resident Welfare Associations (RWAs) to set up community gardens and compost units, reducing waste burning. By enforcing environmental laws and creating public awareness on sustainability, the department aims to reduce the carbon footprint, improve air and water quality, and create a healthier, greener living space for the residents of New Delhi. It monitors commercial exhaust emissions, conducts soil health checks in green belts, and coordinates noise barrier installations near schools and hospitals.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_UTILITIES_ELEC_DESC = """
The Public Utilities and Electrical Infrastructure Department is responsible for planning, coordinating, and monitoring the delivery of essential utility services, focusing on electrical infrastructure, power grids, and domestic gas distribution networks across the New Delhi constituency. The department's main goal is to ensure uninterrupted supply of power and other basic utility services to all households, commercial establishments, and public institutions. It acts as the primary monitoring authority for the electrical distribution network, coordinating with power discoms such as BSES Rajdhani Power, BSES Yamuna Power, and the NDMC Power Department. The department's scope includes managing power distribution capacity, resolving high-voltage fluctuations, coordinates transformer installations, supervising underground utility cabling, and monitoring safety standards around power lines. It handles complaints related to frequent power outages, damaged electrical transformers, dangling high-tension wires, billing disputes, and delay in new power connection installations. The department also coordinates with Indraprastha Gas Limited (IGL) to monitor the expansion and safety of the Piped Natural Gas (PNG) network. It ensures that utility trenching works on roads are properly coordinated and repaired promptly. By maintaining a robust electrical grid and streamlined utility services, the department aims to support the energy demands of the constituency, minimize power cuts, and ensure public safety near electrical installations. It holds monthly utility review boards, audits grid capacity, and coordinates safety clearances for utility tunnels.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_STREETLIGHTS_DESC = """
The Electricity Supply and Streetlight Maintenance Department is a specialized sub-department dedicated to maintaining the public lighting grid, illuminating dark spots, and resolving local power distribution issues in the New Delhi constituency. Proper streetlighting is critical for public safety, preventing crime, and ensuring safe pedestrian and vehicular movement at night. The department's core responsibilities include maintaining all streetlights, replacing broken bulbs with energy-efficient LED lights, installing high-mast lights at major junctions, repairing damaged electric poles, and addressing local electricity outages. The department works in close partnership with discoms like BSES Rajdhani, BSES Yamuna, NDMC Power, and PWD. It handles citizen complaints regarding non-functional streetlights, dark stretches on public roads, flickering street lamps, damaged electricity meters on poles, electric shock hazards from exposed wires, and power cuts in residential areas. The department's standard operating procedures involve conducting regular night surveys to identify dark spots, implementing a 24-hour grievance resolution target for broken streetlights, and upgrading old sodium lamps to smart LED lighting systems. The department also coordinates the installation of solar-powered streetlights in parks and community areas to promote renewable energy. By ensuring well-lit streets and a safe electrical grid, the department strives to eliminate dark spots and enhance security for all residents, particularly women and children. It manages digital streetlight controllers, schedules regular insulation testing of poles, and tracks discom maintenance logs to ensure optimal grid uptime.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_WATER_DIST_DESC = """
The Drinking Water Distribution and Tube-well Operations Department is responsible for managing the drinking water supply network, maintaining municipal tube-wells and borewells, and coordinating emergency water distribution in the New Delhi constituency. Access to clean and sufficient drinking water is essential for public health and daily life. The department's duties include monitoring water supply schedules, repairing leaks in water distribution pipelines, maintaining and operating groundwater tube-wells, testing water quality, and dispatching emergency water tankers to water-scarce areas. The department works in close coordination with the Delhi Jal Board (DJB) and local municipal bodies. It handles citizen complaints regarding dirty or contaminated water supply, low water pressure, broken water mains, dry tube-wells, water tanker booking requests, and overall water scarcity in residential colonies. The department's operational workflow includes regular water quality testing at distribution endpoints to prevent contamination, maintaining a fleet of GPS-tracked water tankers, and planning new water pipelines in underserved areas. It also promotes rainwater harvesting and wastewater recycling initiatives to conserve water resources and manage the groundwater table. By ensuring a reliable, clean, and equitable water supply, the department aims to eradicate water scarcity and prevent waterborne diseases in the constituency. It coordinates regular cleaning of underground reservoirs, audits industrial water meters, and operates filtration plants to provide safe and healthy drinking water to every household.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_UTILITIES_LIAISON_DESC = """
The Public Utility Infrastructure Liaison Department is responsible for coordinating all underground and overhead utility works, managing road-cutting permissions, and coordinating the restoration of public roads after utility installations in the New Delhi constituency. The department acts as the single point of contact for utility service providers, telecom companies, and municipal authorities to prevent damage to public infrastructure during cabling and piping works. The department's scope includes liaising with Indraprastha Gas Limited (IGL) for gas pipelines, MTNL and private telecom operators for optical fiber cables, and Delhi Jal Board for sewer/water lines. It handles complaints related to un-restored utility trenches, damaged water pipes due to road digging, illegal overhead cables, open utility chambers, and traffic disruptions caused by coordinate road excavation. The department's standard operating procedures include enforcing the 'Dig Once' policy, ensuring that telecom and utility companies obtain proper permissions, and monitoring that contractors restore excavated roads to their original condition within a specified SLA. By streamlining utility coordination, the department aims to protect public roads from frequent damage, minimize traffic disruptions, and ensure the safe installation of utility infrastructure. It maintains a centralized utility map, conducts post-restoration quality audits, and enforces penalty clauses on service providers who violate road-cutting regulations.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_WELFARE_OUTREACH_DESC = """
The Public Relations, Social Welfare and Outreach Department is the primary public-facing division of the MP Office, dedicated to managing citizen communication, facilitating social welfare schemes, and conducting community outreach programs in the New Delhi constituency. The department serves as the direct link between the Member of Parliament and the constituents. Its main objective is to make governance accessible, transparent, and responsive to the needs of the public. The department's responsibilities include managing the MP's public schedule, organizing regular Jan Sabhas (public grievance hearings), managing the citizen helpdesk and helpline, and running public awareness campaigns about central and state government programs. It coordinates closely with the Ministry of Social Justice and Empowerment, local NGOs, and Resident Welfare Associations (RWAs). The department handles citizen requests for developmental funds, recommendations for medical assistance, complaints regarding non-receipt of welfare benefits, and general public feedback. Its operational workflow includes documenting all grievances received during Jan Sabhas, routing them to the respective departments, and sending status updates to the complainants. The department also coordinates health camps, food distribution drives, and community workshops for marginalized sections. By maintaining open communication and proactive outreach, the department aims to foster trust, empower citizens, and ensure that social welfare benefits reach the grass-roots level. It manages digital grievance portals, coordinates community newsletters, and tracks public sentiment to continuously improve representative services.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_WELFARE_SCHEMES_DESC = """
The Welfare Scheme Benefits and DBT Facilitation Department is a specialized unit dedicated to helping eligible residents register for and receive benefits under various central and state government social welfare schemes in the New Delhi constituency. The department focuses on financial inclusion and social security for vulnerable populations, including senior citizens, widows, persons with disabilities, and street vendors. Its responsibilities include coordinating registration camps for old-age and widow pensions, assisting in Ayushman Bharat health card applications, facilitating PM SVANidhi loans for street vendors, processing ration card applications, and helping citizens secure disability certificates. The department coordinates with banks, the Department of Social Welfare, and the Ministry of Electronics and Information Technology (MeitY) to resolve Direct Benefit Transfer (DBT) issues. It handles citizen grievances regarding delayed pension payments, rejected scheme applications, lack of information about eligibility, difficulties in updating Aadhaar details for schemes, and issues with ration distribution. The department's standard procedures include hosting bi-weekly registration camps in various municipal wards, operating help kiosks at the MP Office, and tracking application approvals. By simplifying the registration process and resolving transaction bottlenecks, the department ensures that social security benefits are delivered directly and transparently to those who need them most. It operates mobile registration vans, manages scheme helpdesks at local markets, and audits DBT transaction logs to ensure seamless benefits distribution.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_PR_OUTREACH_DESC = """
The Public Relations, Jan Sabhas and Grievance Desk Department is responsible for managing media communications, coordinating public meetings, and operating the physical citizen helpdesk at the New Delhi constituency office. The department's primary mission is to ensure that the MP Office remains accessible to every resident and that public feedback is systematically integrated into local governance. Its responsibilities include organizing Jan Sabhas (town hall meetings) in different neighborhoods, managing the daily visitor intake at the grievance desk, conducting citizen feedback surveys, and maintaining media relations. The department coordinates with local resident welfare associations, trade unions, and market associations to schedule community meetings. It handles citizen requests for appointments with the MP, public feedback on municipal issues, suggestions for local area development projects, and grievances regarding slow administrative responses. The department's standard operating procedures involve logging every visitor's details and grievance into a digital tracking system, distributing grievance cards, and scheduling follow-up communications. By organizing structured public hearings and keeping a transparent feedback loop, the department ensures that the representative office remains highly responsive to the evolving needs of the constituency. It coordinates press conferences, publishes monthly constituency developmental briefs, and manages digital feedback surveys to capture public needs and opinions.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_GENDER_AGE_WELFARE_DESC = """
The Women, Children and Senior Citizen Welfare Department is dedicated to addressing the specific safety, developmental, and social needs of women, children, and the elderly in the New Delhi constituency. The department works to create a secure and supportive environment that empowers women, protects children, and provides dignity to senior citizens. Its operational scope includes coordinating self-defense training programs for women, monitoring safety parameters in public transport, maintaining daycare and playschool infrastructure, developing senior citizen recreational centers, and organizing health and digital literacy workshops for the elderly. The department collaborates with the Department of Women and Child Development, local police units, and senior citizen associations. It handles complaints related to unsafe public stretches, poorly maintained children's parks, lack of basic amenities in municipal playschools (anganwadis), elder abuse, and requests for senior citizen identity cards and recreation facilities. The department's standard procedures involve auditing safety parameters (such as lighting and CCTV coverage) at public transit hubs, organizing health check-up camps for senior citizens, and maintaining a helpline for emergency coordination. By developing targeted welfare initiatives, the department aims to enhance the safety, well-being, and social integration of these vital groups in the community. It funds women's self-help groups, manages mobile medical checkup clinics for the elderly, and coordinates community playschool upgrades to foster healthy growth.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_SAFETY_TRAFFIC_DESC = """
The Civic Safety, Traffic Coordination and Emergency Services Department is responsible for planning, coordinating, and implementing safety measures, traffic management solutions, and emergency response systems across the New Delhi constituency. The department's primary objective is to protect public life and property by addressing safety hazards, coordinating traffic flow, and preparing for natural or man-made disasters. Given that New Delhi is a high-security and high-traffic zone containing government offices, embassies, and major commercial markets, maintaining civic safety is of paramount importance. The department serves as the key coordination link between the MP Office and various enforcement agencies, including the Delhi Police, the Delhi Traffic Police, the Delhi Fire Service, the National Disaster Response Force (NDRF), and municipal disaster management cells. Its responsibilities include analyzing road safety data, identifying accident-prone blackspots, coordinates structural safety audits of old buildings, planning emergency evacuation routes, and organizing disaster preparedness drills. The department handles complaints related to safety hazards in public areas, traffic congestion on major corridors, illegal parking, and lack of emergency services coordination. By fostering inter-agency coordination and community preparedness, the department aims to create a secure, hazard-free, and well-managed urban environment. It chairs safety review committees, audits building structural designs, and manages safety surveillance networks to protect the community.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_TRAFFIC_COORD_DESC = """
The Delhi Police and Traffic Management Coordination Department is a specialized sub-department dedicated to coordinates traffic management, road safety, and civic policing coordination in the New Delhi constituency. The department's main objective is to reduce traffic congestion, prevent accidents, and enhance public security through better coordination with law enforcement agencies. Its operational scope includes identifying traffic bottlenecks, requesting the installation of traffic signals and speed breakers, planning pedestrian crossings and foot-over-bridges, monitoring the installation of public safety CCTV cameras, and coordinating action against illegal parking and encroached footpaths. The department works in close coordination with the Delhi Traffic Police, local Delhi Police stations, PWD, and municipal bodies. It handles citizen complaints regarding daily traffic jams, lack of traffic police presence at busy intersections, non-functional traffic signals, unsafe pedestrian areas, illegal commercial parking on public roads, and request for new speed breakers or street lights. The department's standard procedures include conducting joint field inspections with traffic police, analyzing congestion data to modify traffic plans, and organizing road safety awareness campaigns in schools and commercial markets. By optimizing traffic flow and improving surveillance, the department strives to ensure a safe and smooth travel experience for all commuters. It manages traffic diversion approvals, coordinates public parking layout audits, and tracks traffic discom metrics to improve road management.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_DISASTER_EMERG_DESC = """
The Disaster Management and Emergency Relief Operations Department is responsible for emergency preparedness, coordinates rescue operations, and organizing relief distribution during disasters in the New Delhi constituency. New Delhi is located in Seismic Zone IV (high risk of earthquakes) and experiences high summer temperatures, seasonal monsoonal waterlogging, and fire hazards in congested areas, necessitating a high state of emergency readiness. The department's duties include developing disaster management plans, maintaining emergency relief shelters, coordinates with civil defense volunteers, stocking emergency supplies (food, water, medicine, blankets), and organizing first aid and rescue training camps. The department coordinates with the Delhi Disaster Management Authority (DDMA), fire services, red cross, and municipal health departments. It handles citizen reports regarding safety hazards (like weak structures or hazardous trees), requests for emergency relief during fire accidents or building collapses, and queries about emergency helpline services. The department's standard operating procedures involve establishing a 24/7 emergency control room at the MP Office, coordinating quick-response teams during disasters, and managing rehabilitation camps for affected families. By strengthening community-level preparedness and maintaining a quick response mechanism, the department aims to minimize loss of life and property during any unforeseen emergency. It maintains emergency communication equipment, conducts community evacuation drills, and monitors relief stock inventories.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    private static final String DPT_FIRE_SAFETY_DESC = """
The Fire Safety and Structural Auditing Liaison Department is responsible for coordinates safety inspections, monitoring fire safety compliance, and coordinating structural audits of old or weak buildings in the New Delhi constituency. Fire accidents and building collapses in congested residential and commercial hubs present a major threat to public safety. The department's mission is to proactively identify and mitigate these risks by coordinating safety audits. Its operational scope includes inspecting fire exit blockages in markets, checking the availability of functional fire hydrants, coordinating with Delhi Fire Service for fire NOC clearances, and identifying structurally weak buildings that require immediate reinforcement or evacuation. The department collaborates with the Delhi Fire Service, the building departments of MCD and NDMC, and PWD structural engineers. It handles complaints related to violations of fire safety norms in commercial buildings, blocked fire escapes, weak building walls or cracked structures, open electrical switchboards that pose a fire risk, and requests for fire safety inspections. The department's standard operating procedures include conducting joint safety audits in high-risk zones, issuing notices to property owners violating safety guidelines, and conducting mock fire drills for residents and traders. By enforcing strict safety parameters, the department aims to prevent fire disasters and building collapses, protecting the lives of residents and visitors. It manages fire safety awareness programs, compiles structural inspection databases, and tracks compliance filings.

Operationally, this department is governed by a strict citizen services charter that dictates precise response times and quality metrics for all logged issues. The standard operating procedure requires that upon receiving a new ticket—whether routed automatically by the AI system, submitted via the mobile citizen application, or entered manually at the constituency helpdesk—an initial feasibility assessment is conducted within twenty-four hours. For minor grievances and maintenance requests, field inspectors are dispatched to the physical site to verify the complaint details and log GPS-tagged photographs. A detailed repair estimate and work schedule are then compiled and submitted to the department head for immediate financial approval. If the project requires execution through external agencies, a fast-track tendering process is initiated. The department maintains a panel of pre-approved contractors to minimize delays. For major infrastructure works or projects involving significant capital allocation, detailed engineering drawings and environmental impact assessments are prepared and reviewed by a joint technical committee before work begins.
To ensure seamless execution, this division holds bi-weekly coordination meetings with secondary agencies such as the Municipal Corporation of Delhi, the New Delhi Municipal Council, the Delhi Development Authority, and various local police and fire services. These coordination meetings help resolve jurisdictional disputes and expedite permission approvals, such as road-cutting or tree-trimming clearances. The department enforces strict quality control guidelines, conducting random material testing and structural integrity audits at various stages of construction. Work progress is tracked on a digital dashboard, and contractors are penalized for any unexcused delays. Upon project completion, a joint inspection is carried out by department engineers and local Resident Welfare Association representatives to ensure that the work meets the required standards before final payments are released.
The escalation matrix is structured to ensure accountability. If a ticket is not resolved within the defined Service Level Agreement timeframe, it is automatically escalated to the assistant commissioner, and subsequently to the department director if the delay persists. Monthly performance audits are conducted, and reports detailing resolution rates, average turnaround times, and citizen feedback ratings are prepared for review by the Member of Parliament. Furthermore, the department actively participates in monthly public outreach programs, conducting open hearings and feedback surveys to understand the evolving needs of the constituency. Through this comprehensive operational framework, the department is committed to delivering high-quality, transparent, and responsive public services that enhance the urban infrastructure and civic well-being of the New Delhi constituency.
""";

    public DataSeeder(OrganizationRepository organizations, OrganizationConfigRepository orgConfigs,
                      DepartmentRepository departments, OfficerRepository officers,
                      CitizenProfileRepository citizens, PointActivityRepository activities,
                      TaskRepository tasks, TaskAssignmentRepository taskAssignments,
                      ReportingHierarchyRepository reportingHierarchies, TransitPassRepository transitPasses,
                      AiChatMessageRepository aiChatMessages) {
        this.organizations = organizations;
        this.orgConfigs = orgConfigs;
        this.departments = departments;
        this.officers = officers;
        this.citizens = citizens;
        this.activities = activities;
        this.tasks = tasks;
        this.taskAssignments = taskAssignments;
        this.reportingHierarchies = reportingHierarchies;
        this.transitPasses = transitPasses;
        this.aiChatMessages = aiChatMessages;
    }

    @Override
    public void run(String... args) {
        // Drop all collections on startup to seed from scratch
        organizations.deleteAll();
        orgConfigs.deleteAll();
        departments.deleteAll();
        officers.deleteAll();
        citizens.deleteAll();
        activities.deleteAll();
        tasks.deleteAll();
        taskAssignments.deleteAll();
        reportingHierarchies.deleteAll();
        transitPasses.deleteAll();
        aiChatMessages.deleteAll();

        String orgId = seedOrganization();
        seedConfig(orgId);
        seedDepartmentsAndOfficers(orgId);
        seedCitizen();
        seedTasks(orgId);
    }

    private String seedOrganization() {
        // Polygon boundary covering New Delhi constituency [lng, lat]
        GeoPolygon boundary = new GeoPolygon("Polygon", List.of(List.of(
                List.of(77.09, 28.51),
                List.of(77.27, 28.51),
                List.of(77.27, 28.69),
                List.of(77.09, 28.69),
                List.of(77.09, 28.51)
        )));

        Organization org = Organization.builder()
                .name("MP Office of New Delhi")
                .description(ORG_DESC)
                .createdAt(1791244800000L)
                .constituency(new Organization.OrgConstituency("New Delhi Constituency", boundary))
                .build();

        org = organizations.save(org);
        return org.getId();
    }

    private void seedConfig(String orgId) {
        orgConfigs.save(OrganizationConfig.builder()
                .orgId(orgId)
                .categories(List.of(
                        new OrganizationConfig.OrgCategory("INFRASTRUCTURE", "Infrastructure",
                                "Roads, parks, streetlights, municipal libraries, and public works under development"),
                        new OrganizationConfig.OrgCategory("SANITATION", "Sanitation",
                                "Garbage cleanup, municipal waste, pest control, public health, and local cleanliness"),
                        new OrganizationConfig.OrgCategory("UTILITIES", "Utilities",
                                "Water supply, water logging, sewerage lines, electricity grids, and street lighting"),
                        new OrganizationConfig.OrgCategory("WELFARE", "Welfare Assistance",
                                "Support with pensions, scholarship applications, Ayushman cards, and central welfare scheme enrollment"),
                        new OrganizationConfig.OrgCategory("CIVIC_GRIEVANCE", "Civic Grievance",
                                "General civic issues, public disputes, liaison requests, and coordination with Delhi agencies (MCD/NDMC/DDA)")
                ))
                .priorities(List.of("P0", "P1", "P2", "P3"))
                .statuses(List.of("OPEN", "IN_PROGRESS", "RESOLVED", "REJECTED"))
                .updatedAt(System.currentTimeMillis())
                .customPromptExtension("Prioritise public-safety and central welfare assistance concerns as P0.")
                .build());
    }

    private Department createDept(String orgId, String name, String parentId, String parentName, int depth, String baseDesc, GeoPolygon boundary, List<Department> list) {
        String description = baseDesc + "\n\nSpecialized sub-department focused on: " + name;
        Department dept = departments.save(Department.builder()
                .orgId(orgId)
                .name(name)
                .parentDepartmentId(parentId)
                .parentDepartmentName(parentName)
                .depth(depth)
                .officerCount(0)
                .roleDescription(description)
                .constituency(new Department.DepartmentConstituency(name + " Jurisdiction Zone", boundary))
                .build());
        list.add(dept);
        return dept;
    }

    private List<Department> getChildren(Department parent, List<Department> allDepts) {
        return allDepts.stream()
                .filter(d -> parent.getId().equals(d.getParentDepartmentId()))
                .toList();
    }

    private void linkOfficerToDepts(Officer officer, List<Department> depts) {
        List<String> deptIds = depts.stream().map(Department::getId).toList();
        officer.setDepartmentIds(new ArrayList<>(deptIds));
        officers.save(officer);

        for (Department d : depts) {
            d.setHeadOfficerId(officer.getId());
            d.setHeadOfficerName(officer.getName());
            d.setOfficerCount(1);
            departments.save(d);
        }
    }

    private void seedDepartmentsAndOfficers(String orgId) {
        // Boundary covering the entire New Delhi constituency
        GeoPolygon boundary = new GeoPolygon("Polygon", List.of(List.of(
                List.of(77.09, 28.51),
                List.of(77.27, 28.51),
                List.of(77.27, 28.69),
                List.of(77.09, 28.69),
                List.of(77.09, 28.51)
        )));

        List<Department> allCreatedDepts = new ArrayList<>();

        // --- ROOT 1: MPLADS & Infrastructure Development
        Department dptMplads = createDept(orgId, "MPLADS & Infrastructure Development", null, null, 0, DPT_MPLADS_DESC, boundary, allCreatedDepts);
        
        Department subDpt1_1 = createDept(orgId, "Public Works & Civic Amenities", dptMplads.getId(), dptMplads.getName(), 1, DPT_PW_AMENITIES_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Parks & Open Gymnasiums Maintenance", subDpt1_1.getId(), subDpt1_1.getName(), 2, DPT_PW_AMENITIES_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Public Seating & Community Shelters", subDpt1_1.getId(), subDpt1_1.getName(), 2, DPT_PW_AMENITIES_DESC, boundary, allCreatedDepts);
        
        Department subDpt1_2 = createDept(orgId, "Education & Healthcare Infrastructure", dptMplads.getId(), dptMplads.getName(), 1, DPT_EDU_HEALTH_DESC, boundary, allCreatedDepts);
        createDept(orgId, "School Buildings Renewal", subDpt1_2.getId(), subDpt1_2.getName(), 2, DPT_EDU_HEALTH_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Primary Health Centers Construction", subDpt1_2.getId(), subDpt1_2.getName(), 2, DPT_EDU_HEALTH_DESC, boundary, allCreatedDepts);
        
        Department subDpt1_3 = createDept(orgId, "Urban Renewal & Heritage Preservation", dptMplads.getId(), dptMplads.getName(), 1, DPT_URBAN_HERITAGE_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Monument Lighting & Aesthetics", subDpt1_3.getId(), subDpt1_3.getName(), 2, DPT_URBAN_HERITAGE_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Historic Markets Facade Control", subDpt1_3.getId(), subDpt1_3.getName(), 2, DPT_URBAN_HERITAGE_DESC, boundary, allCreatedDepts);
        
        Department subDpt1_4 = createDept(orgId, "MLA Local Area Development Scheme Coordination", dptMplads.getId(), dptMplads.getName(), 1, DPT_MPLADS_DESC, boundary, allCreatedDepts);
        createDept(orgId, "MLA Constituency Fund Audit", subDpt1_4.getId(), subDpt1_4.getName(), 2, DPT_MPLADS_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Local Ward Development Planning", subDpt1_4.getId(), subDpt1_4.getName(), 2, DPT_MPLADS_DESC, boundary, allCreatedDepts);

        // --- ROOT 2: Public Grievance, Sanitation & Waste Management
        Department dptGrievance = createDept(orgId, "Public Grievance, Sanitation & Waste Management", null, null, 0, DPT_SAN_GRIEV_DESC, boundary, allCreatedDepts);
        
        Department subDpt2_1 = createDept(orgId, "Municipal Cleanliness & Solid Waste Management", dptGrievance.getId(), dptGrievance.getName(), 1, DPT_WASTE_MGMT_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Garbage Collection & Transportation", subDpt2_1.getId(), subDpt2_1.getName(), 2, DPT_WASTE_MGMT_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Street Sweeping & Debris Removal", subDpt2_1.getId(), subDpt2_1.getName(), 2, DPT_WASTE_MGMT_DESC, boundary, allCreatedDepts);
        
        Department subDpt2_2 = createDept(orgId, "Drainage, Sewerage & Water Logging Control", dptGrievance.getId(), dptGrievance.getName(), 1, DPT_DRAINAGE_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Sewer Line Desilting & Repair", subDpt2_2.getId(), subDpt2_2.getName(), 2, DPT_DRAINAGE_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Stormwater Drain Maintenance", subDpt2_2.getId(), subDpt2_2.getName(), 2, DPT_DRAINAGE_DESC, boundary, allCreatedDepts);
        
        Department subDpt2_3 = createDept(orgId, "Environmental Protection & Pollution Control", dptGrievance.getId(), dptGrievance.getName(), 1, DPT_ENV_POLLUTION_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Air Quality Monitoring & Dust Control", subDpt2_3.getId(), subDpt2_3.getName(), 2, DPT_ENV_POLLUTION_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Noise & Industrial Emissions Liaison", subDpt2_3.getId(), subDpt2_3.getName(), 2, DPT_ENV_POLLUTION_DESC, boundary, allCreatedDepts);

        // --- ROOT 3: Public Utilities & Electrical Infrastructure
        Department dptUtilities = createDept(orgId, "Public Utilities & Electrical Infrastructure", null, null, 0, DPT_UTILITIES_ELEC_DESC, boundary, allCreatedDepts);
        
        Department subDpt3_1 = createDept(orgId, "Electricity Supply & Streetlight Maintenance", dptUtilities.getId(), dptUtilities.getName(), 1, DPT_STREETLIGHTS_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Streetlight Bulb & Cable Repair", subDpt3_1.getId(), subDpt3_1.getName(), 2, DPT_STREETLIGHTS_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Transformer & Grid Liaison", subDpt3_1.getId(), subDpt3_1.getName(), 2, DPT_STREETLIGHTS_DESC, boundary, allCreatedDepts);
        
        Department subDpt3_2 = createDept(orgId, "Drinking Water Distribution & Tube-well Operations", dptUtilities.getId(), dptUtilities.getName(), 1, DPT_WATER_DIST_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Water Tanker Emergency Supply", subDpt3_2.getId(), subDpt3_2.getName(), 2, DPT_WATER_DIST_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Pipeline Leakage & Valve Control", subDpt3_2.getId(), subDpt3_2.getName(), 2, DPT_WATER_DIST_DESC, boundary, allCreatedDepts);
        
        Department subDpt3_3 = createDept(orgId, "Public Utility Infrastructure Liaison", dptUtilities.getId(), dptUtilities.getName(), 1, DPT_UTILITIES_LIAISON_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Road Trenching Approvals", subDpt3_3.getId(), subDpt3_3.getName(), 2, DPT_UTILITIES_LIAISON_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Optical Fiber & Cable Duct Coordination", subDpt3_3.getId(), subDpt3_3.getName(), 2, DPT_UTILITIES_LIAISON_DESC, boundary, allCreatedDepts);

        // --- ROOT 4: Public Relations, Social Welfare & Outreach
        Department dptWelfare = createDept(orgId, "Public Relations, Social Welfare & Outreach", null, null, 0, DPT_WELFARE_OUTREACH_DESC, boundary, allCreatedDepts);
        
        Department subDpt4_1 = createDept(orgId, "Welfare Scheme Benefits & DBT Facilitation", dptWelfare.getId(), dptWelfare.getName(), 1, DPT_WELFARE_SCHEMES_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Old Age & Disability Pension Helpdesk", subDpt4_1.getId(), subDpt4_1.getName(), 2, DPT_WELFARE_SCHEMES_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Ayushman Bharat Scheme Enrollment", subDpt4_1.getId(), subDpt4_1.getName(), 2, DPT_WELFARE_SCHEMES_DESC, boundary, allCreatedDepts);
        
        Department subDpt4_2 = createDept(orgId, "Public Relations, Jan Sabhas & Grievance Desk", dptWelfare.getId(), dptWelfare.getName(), 1, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Town Hall Meetings Coordination", subDpt4_2.getId(), subDpt4_2.getName(), 2, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Digital Public Relations", subDpt4_2.getId(), subDpt4_2.getName(), 2, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        
        Department subDpt4_3 = createDept(orgId, "Women, Children & Senior Citizen Welfare", dptWelfare.getId(), dptWelfare.getName(), 1, DPT_GENDER_AGE_WELFARE_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Daycare & Anganwadi Infrastructure", subDpt4_3.getId(), subDpt4_3.getName(), 2, DPT_GENDER_AGE_WELFARE_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Senior Citizen Recreation Centers", subDpt4_3.getId(), subDpt4_3.getName(), 2, DPT_GENDER_AGE_WELFARE_DESC, boundary, allCreatedDepts);

        // --- ROOT 5: Civic Safety, Traffic Coordination & Emergency Services
        Department dptSafety = createDept(orgId, "Civic Safety, Traffic Coordination & Emergency Services", null, null, 0, DPT_SAFETY_TRAFFIC_DESC, boundary, allCreatedDepts);
        
        Department subDpt5_1 = createDept(orgId, "Delhi Police & Traffic Management Coordination", dptSafety.getId(), dptSafety.getName(), 1, DPT_TRAFFIC_COORD_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Traffic Light Synchronization", subDpt5_1.getId(), subDpt5_1.getName(), 2, DPT_TRAFFIC_COORD_DESC, boundary, allCreatedDepts);
        createDept(orgId, "CCTV Surveillance Network Management", subDpt5_1.getId(), subDpt5_1.getName(), 2, DPT_TRAFFIC_COORD_DESC, boundary, allCreatedDepts);
        
        Department subDpt5_2 = createDept(orgId, "Disaster Management & Emergency Relief Operations", dptSafety.getId(), dptSafety.getName(), 1, DPT_DISASTER_EMERG_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Emergency Evacuation & Shelter Coordination", subDpt5_2.getId(), subDpt5_2.getName(), 2, DPT_DISASTER_EMERG_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Disaster Relief Fund Distribution", subDpt5_2.getId(), subDpt5_2.getName(), 2, DPT_DISASTER_EMERG_DESC, boundary, allCreatedDepts);
        
        Department subDpt5_3 = createDept(orgId, "Fire Safety & Structural Auditing Liaison", dptSafety.getId(), dptSafety.getName(), 1, DPT_FIRE_SAFETY_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Fire Hydrant Placement & Audits", subDpt5_3.getId(), subDpt5_3.getName(), 2, DPT_FIRE_SAFETY_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Dilapidated Buildings Safety Audits", subDpt5_3.getId(), subDpt5_3.getName(), 2, DPT_FIRE_SAFETY_DESC, boundary, allCreatedDepts);

        // --- ROOT 6: Lok Sabha & MLA Constituency Coordination
        Department dptConstituency = createDept(orgId, "Lok Sabha & MLA Constituency Coordination", null, null, 0, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        
        Department subDpt6_1 = createDept(orgId, "MLA & Local Bodies Liaison", dptConstituency.getId(), dptConstituency.getName(), 1, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        createDept(orgId, "NDMC & MCD Joint Working Group", subDpt6_1.getId(), subDpt6_1.getName(), 2, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Cantonment Board Coordination", subDpt6_1.getId(), subDpt6_1.getName(), 2, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        
        Department subDpt6_2 = createDept(orgId, "Citizen Complaints Resolution Desk", dptConstituency.getId(), dptConstituency.getName(), 1, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Ward-level Grievance Officers", subDpt6_2.getId(), subDpt6_2.getName(), 2, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        createDept(orgId, "MLA Special Cell Helpline", subDpt6_2.getId(), subDpt6_2.getName(), 2, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);

        // --- ROOT 7: Information Technology & Smart City Operations
        Department dptIT = createDept(orgId, "Information Technology & Smart City Operations", null, null, 0, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        
        Department subDpt7_1 = createDept(orgId, "Citizen Mobile Apps & Portals Support", dptIT.getId(), dptIT.getName(), 1, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Praja Disha Mobile App Maintenance", subDpt7_1.getId(), subDpt7_1.getName(), 2, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        createDept(orgId, "AI Chatbot Triage Optimization", subDpt7_1.getId(), subDpt7_1.getName(), 2, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        
        Department subDpt7_2 = createDept(orgId, "Smart City Sensors & IoT Deployment", dptIT.getId(), dptIT.getName(), 1, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Smart Waste Bin Sensors", subDpt7_2.getId(), subDpt7_2.getName(), 2, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);
        createDept(orgId, "Environmental Sensor Grid Maintenance", subDpt7_2.getId(), subDpt7_2.getName(), 2, DPT_PR_OUTREACH_DESC, boundary, allCreatedDepts);

        // --- Create Officers and dynamically link them ---

        Officer offAarav = officers.save(Officer.builder()
                .orgIds(List.of(orgId))
                .officerUserName("aarav_sharma")
                .name("Aarav Sharma")
                .email("aarav.sharma@delhimpoffice.gov.in")
                .phone("9999988888") // Default Demo Login
                .departmentIds(new ArrayList<>())
                .isActive(true)
                .createdAt(System.currentTimeMillis())
                .build());

        Officer offAnanya = officers.save(Officer.builder()
                .orgIds(List.of(orgId))
                .officerUserName("ananya_sen")
                .name("Ananya Sen")
                .email("ananya.sen@delhimpoffice.gov.in")
                .phone("9810012346")
                .departmentIds(new ArrayList<>())
                .isActive(true)
                .managerUserNames(new ArrayList<>(List.of("aarav_sharma")))
                .createdAt(System.currentTimeMillis())
                .build());

        Officer offVikram = officers.save(Officer.builder()
                .orgIds(List.of(orgId))
                .officerUserName("vikram_singh")
                .name("Vikramaditya Singh")
                .email("vikram.singh@delhimpoffice.gov.in")
                .phone("9810012347")
                .departmentIds(new ArrayList<>())
                .isActive(true)
                .createdAt(System.currentTimeMillis())
                .build());

        Officer offPriya = officers.save(Officer.builder()
                .orgIds(List.of(orgId))
                .officerUserName("priya_iyer")
                .name("Priya Iyer")
                .email("priya.iyer@delhimpoffice.gov.in")
                .phone("9810012348")
                .departmentIds(new ArrayList<>())
                .isActive(true)
                .managerUserNames(new ArrayList<>(List.of("vikram_singh")))
                .createdAt(System.currentTimeMillis())
                .build());

        Officer offRohan = officers.save(Officer.builder()
                .orgIds(List.of(orgId))
                .officerUserName("rohan_mehta")
                .name("Rohan Mehta")
                .email("rohan.mehta@delhimpoffice.gov.in")
                .phone("9810012349")
                .departmentIds(new ArrayList<>())
                .isActive(true)
                .createdAt(System.currentTimeMillis())
                .build());

        Officer offAmit = officers.save(Officer.builder()
                .orgIds(List.of(orgId))
                .officerUserName("amit_kumar")
                .name("Amit Kumar")
                .email("amit.kumar@delhimpoffice.gov.in")
                .phone("9810012350")
                .departmentIds(new ArrayList<>())
                .isActive(true)
                .managerUserNames(new ArrayList<>(List.of("rohan_mehta")))
                .createdAt(System.currentTimeMillis())
                .build());

        Officer offSanjay = officers.save(Officer.builder()
                .orgIds(List.of(orgId))
                .officerUserName("sanjay_dutt")
                .name("Sanjay Dutt")
                .email("sanjay.dutt@delhimpoffice.gov.in")
                .phone("9810012351")
                .departmentIds(new ArrayList<>())
                .isActive(true)
                .createdAt(System.currentTimeMillis())
                .build());

        Officer offNeha = officers.save(Officer.builder()
                .orgIds(List.of(orgId))
                .officerUserName("neha_gupta")
                .name("Neha Gupta")
                .email("neha.gupta@delhimpoffice.gov.in")
                .phone("9810012352")
                .departmentIds(new ArrayList<>())
                .isActive(true)
                .managerUserNames(new ArrayList<>(List.of("sanjay_dutt")))
                .createdAt(System.currentTimeMillis())
                .build());

        Officer offKaran = officers.save(Officer.builder()
                .orgIds(List.of(orgId))
                .officerUserName("karan_johar")
                .name("Karan Johar")
                .email("karan.johar@delhimpoffice.gov.in")
                .phone("9810012353")
                .departmentIds(new ArrayList<>())
                .isActive(true)
                .createdAt(System.currentTimeMillis())
                .build());

        Officer offDeepika = officers.save(Officer.builder()
                .orgIds(List.of(orgId))
                .officerUserName("deepika_p")
                .name("Deepika Padukone")
                .email("deepika.p@delhimpoffice.gov.in")
                .phone("9810012354")
                .departmentIds(new ArrayList<>())
                .isActive(true)
                .managerUserNames(new ArrayList<>(List.of("karan_johar")))
                .createdAt(System.currentTimeMillis())
                .build());

        Officer offRajesh = officers.save(Officer.builder()
                .orgIds(List.of(orgId))
                .officerUserName("rajesh_kumar")
                .name("Rajesh Kumar")
                .email("rajesh.kumar@delhimpoffice.gov.in")
                .phone("9810012355")
                .departmentIds(new ArrayList<>())
                .isActive(true)
                .createdAt(System.currentTimeMillis())
                .build());

        Officer offKriti = officers.save(Officer.builder()
                .orgIds(List.of(orgId))
                .officerUserName("kriti_verma")
                .name("Kriti Verma")
                .email("kriti.verma@delhimpoffice.gov.in")
                .phone("9810012356")
                .departmentIds(new ArrayList<>())
                .isActive(true)
                .createdAt(System.currentTimeMillis())
                .build());

        // Update departments with their assigned head officer details and link officers to departments
        List<Department> deptsAarav = new ArrayList<>();
        deptsAarav.add(dptMplads);
        deptsAarav.add(subDpt1_1);
        deptsAarav.addAll(getChildren(subDpt1_1, allCreatedDepts));
        linkOfficerToDepts(offAarav, deptsAarav);

        List<Department> deptsAnanya = new ArrayList<>();
        deptsAnanya.add(subDpt1_2);
        deptsAnanya.addAll(getChildren(subDpt1_2, allCreatedDepts));
        deptsAnanya.add(subDpt1_3);
        deptsAnanya.addAll(getChildren(subDpt1_3, allCreatedDepts));
        deptsAnanya.add(subDpt1_4);
        deptsAnanya.addAll(getChildren(subDpt1_4, allCreatedDepts));
        linkOfficerToDepts(offAnanya, deptsAnanya);

        List<Department> deptsVikram = new ArrayList<>();
        deptsVikram.add(dptGrievance);
        deptsVikram.add(subDpt2_1);
        deptsVikram.addAll(getChildren(subDpt2_1, allCreatedDepts));
        linkOfficerToDepts(offVikram, deptsVikram);

        List<Department> deptsPriya = new ArrayList<>();
        deptsPriya.add(subDpt2_2);
        deptsPriya.addAll(getChildren(subDpt2_2, allCreatedDepts));
        deptsPriya.add(subDpt2_3);
        deptsPriya.addAll(getChildren(subDpt2_3, allCreatedDepts));
        linkOfficerToDepts(offPriya, deptsPriya);

        List<Department> deptsSanjay = new ArrayList<>();
        deptsSanjay.add(dptUtilities);
        deptsSanjay.add(subDpt3_1);
        deptsSanjay.addAll(getChildren(subDpt3_1, allCreatedDepts));
        linkOfficerToDepts(offSanjay, deptsSanjay);

        List<Department> deptsNeha = new ArrayList<>();
        deptsNeha.add(subDpt3_2);
        deptsNeha.addAll(getChildren(subDpt3_2, allCreatedDepts));
        deptsNeha.add(subDpt3_3);
        deptsNeha.addAll(getChildren(subDpt3_3, allCreatedDepts));
        linkOfficerToDepts(offNeha, deptsNeha);

        List<Department> deptsRohan = new ArrayList<>();
        deptsRohan.add(dptWelfare);
        deptsRohan.add(subDpt4_1);
        deptsRohan.addAll(getChildren(subDpt4_1, allCreatedDepts));
        linkOfficerToDepts(offRohan, deptsRohan);

        List<Department> deptsAmit = new ArrayList<>();
        deptsAmit.add(subDpt4_2);
        deptsAmit.addAll(getChildren(subDpt4_2, allCreatedDepts));
        deptsAmit.add(subDpt4_3);
        deptsAmit.addAll(getChildren(subDpt4_3, allCreatedDepts));
        linkOfficerToDepts(offAmit, deptsAmit);

        List<Department> deptsKaran = new ArrayList<>();
        deptsKaran.add(dptSafety);
        deptsKaran.add(subDpt5_1);
        deptsKaran.addAll(getChildren(subDpt5_1, allCreatedDepts));
        linkOfficerToDepts(offKaran, deptsKaran);

        List<Department> deptsDeepika = new ArrayList<>();
        deptsDeepika.add(subDpt5_2);
        deptsDeepika.addAll(getChildren(subDpt5_2, allCreatedDepts));
        deptsDeepika.add(subDpt5_3);
        deptsDeepika.addAll(getChildren(subDpt5_3, allCreatedDepts));
        linkOfficerToDepts(offDeepika, deptsDeepika);

        List<Department> deptsRajesh = new ArrayList<>();
        deptsRajesh.add(dptConstituency);
        deptsRajesh.add(subDpt6_1);
        deptsRajesh.addAll(getChildren(subDpt6_1, allCreatedDepts));
        deptsRajesh.add(subDpt6_2);
        deptsRajesh.addAll(getChildren(subDpt6_2, allCreatedDepts));
        linkOfficerToDepts(offRajesh, deptsRajesh);

        List<Department> deptsKriti = new ArrayList<>();
        deptsKriti.add(dptIT);
        deptsKriti.add(subDpt7_1);
        deptsKriti.addAll(getChildren(subDpt7_1, allCreatedDepts));
        deptsKriti.add(subDpt7_2);
        deptsKriti.addAll(getChildren(subDpt7_2, allCreatedDepts));
        linkOfficerToDepts(offKriti, deptsKriti);

        // Adjust officerCount for roots where multiple officers are assigned
        dptMplads.setOfficerCount(2);
        departments.save(dptMplads);

        dptGrievance.setOfficerCount(2);
        departments.save(dptGrievance);

        dptUtilities.setOfficerCount(2);
        departments.save(dptUtilities);

        dptWelfare.setOfficerCount(2);
        departments.save(dptWelfare);

        dptSafety.setOfficerCount(2);
        departments.save(dptSafety);
    }

    private void seedCitizen() {
        citizens.save(CitizenProfile.builder()
                .username("aisha_patel")
                .name("Aisha Patel")
                .email("aisha.patel@example.com")
                .phone("9876543210")
                .points(1250)
                .tier("Silver Citizen")
                .language("en")
                .build());

        activities.save(PointActivity.builder()
                .citizenUserName("aisha_patel")
                .title("Pothole fixed on 5th Ave")
                .source("Verified by City Council")
                .date("Oct 24")
                .points(100)
                .createdAt(System.currentTimeMillis() - 86_400_000L)
                .build());
    }

    private void seedTasks(String orgId) {
        long now = System.currentTimeMillis();

        // Seed Task 1: Broken open gym equipment in Lodhi Gardens
        tasks.save(Task.builder()
                .id("PD-1001")
                .groupId("PD-1001")
                .orgId(orgId)
                .citizenUserName("aisha_patel")
                .title("Broken open gym equipment in Lodhi Gardens")
                .description("The chest press machine in the open gym area of Lodhi Gardens is broken and has loose bolts. This is a safety hazard for children and senior citizens.")
                .voiceUrl("")
                .imageUrl("https://images.unsplash.com/photo-1544033527-b192daee1f5b")
                .language("English")
                .location(new Task.TaskLocation("Lodhi Gardens, near gate number 3, New Delhi",
                        GeoPoint.of(77.2197, 28.5913)))
                .category("Infrastructure")
                .priority("P2")
                .globalStatus("AI-Assigned")
                .isReviewed(false)
                .reporterType("Citizen")
                .dueDate(now + 4L * 24 * 60 * 60 * 1000)
                .createdAt(now - 2L * 24 * 60 * 60 * 1000)
                .comments(new ArrayList<>())
                .notes(new ArrayList<>())
                .activities(new ArrayList<>(List.of(DetailedActivity.builder()
                        .timestamp("Oct 24, 2026, 9:15 AM")
                        .action("AI_ASSIGNED")
                        .performedBy("system_ai")
                        .remarks("Auto-classified as Infrastructure / P2 and routed to Public Works & Civic Amenities")
                        .build())))
                .build());

        // Seed Task 2: Sewage overflow near Karol Bagh Metro Station
        tasks.save(Task.builder()
                .id("PD-1002")
                .groupId("PD-1002")
                .orgId(orgId)
                .citizenUserName("aisha_patel")
                .title("Sewage overflow on main road")
                .description("Main road near Karol Bagh Metro Station is flooded with dirty sewage water leaking from a blocked drain. It smells terrible and is causing heavy traffic congestion.")
                .voiceUrl("")
                .imageUrl("https://images.unsplash.com/photo-1599740831618-24de8c757c91")
                .language("English")
                .location(new Task.TaskLocation("Pusa Road, near Karol Bagh Metro Station, New Delhi",
                        GeoPoint.of(77.1895, 28.6517)))
                .category("Utilities")
                .priority("P1")
                .globalStatus("AI-Assigned")
                .isReviewed(false)
                .reporterType("Citizen")
                .dueDate(now + 2L * 24 * 60 * 60 * 1000)
                .createdAt(now - 1L * 24 * 60 * 60 * 1000)
                .comments(new ArrayList<>())
                .notes(new ArrayList<>())
                .activities(new ArrayList<>(List.of(DetailedActivity.builder()
                        .timestamp("Oct 25, 2026, 11:30 AM")
                        .action("AI_ASSIGNED")
                        .performedBy("system_ai")
                        .remarks("Auto-classified as Utilities / P1 and routed to Utilities & Civil Infrastructure Liaison")
                        .build())))
                .build());

        // Seed TaskAssignments for PD-1001
        java.util.Optional<Department> optMplads = departments.findByOrgId(orgId).stream()
                .filter(d -> "MPLADS & Infrastructure Development".equals(d.getName()))
                .findFirst();
        java.util.Optional<Department> optPw = departments.findByOrgId(orgId).stream()
                .filter(d -> "Public Works & Civic Amenities".equals(d.getName()))
                .findFirst();
        java.util.Optional<Officer> optAnanya = officers.findByOfficerUserName("ananya_sen");

        if (optMplads.isPresent() && optPw.isPresent() && optAnanya.isPresent()) {
            Department root = optMplads.get();
            Department leaf = optPw.get();
            Officer officer = optAnanya.get();
            long assignedTime = now - 2L * 24 * 60 * 60 * 1000;

            taskAssignments.save(gov.prajadisha.backend.task.model.TaskAssignment.builder()
                    .taskId("PD-1001")
                    .departmentId(root.getId())
                    .officerId(root.getHeadOfficerId())
                    .status("PENDING")
                    .assignedAt(assignedTime)
                    .build());

            taskAssignments.save(gov.prajadisha.backend.task.model.TaskAssignment.builder()
                    .taskId("PD-1001")
                    .departmentId(leaf.getId())
                    .officerId(officer.getId())
                    .status("PENDING")
                    .assignedAt(assignedTime)
                    .build());
        }

        // Seed TaskAssignments for PD-1002
        java.util.Optional<Department> optGrievance = departments.findByOrgId(orgId).stream()
                .filter(d -> "Public Grievance, Sanitation & Waste Management".equals(d.getName()))
                .findFirst();
        java.util.Optional<Department> optDrainage = departments.findByOrgId(orgId).stream()
                .filter(d -> "Drainage, Sewerage & Water Logging Control".equals(d.getName()))
                .findFirst();
        java.util.Optional<Officer> optPriya = officers.findByOfficerUserName("priya_iyer");

        if (optGrievance.isPresent() && optDrainage.isPresent() && optPriya.isPresent()) {
            Department root = optGrievance.get();
            Department leaf = optDrainage.get();
            Officer officer = optPriya.get();
            long assignedTime = now - 1L * 24 * 60 * 60 * 1000;

            taskAssignments.save(gov.prajadisha.backend.task.model.TaskAssignment.builder()
                    .taskId("PD-1002")
                    .departmentId(root.getId())
                    .officerId(root.getHeadOfficerId())
                    .status("PENDING")
                    .assignedAt(assignedTime)
                    .build());

            taskAssignments.save(gov.prajadisha.backend.task.model.TaskAssignment.builder()
                    .taskId("PD-1002")
                    .departmentId(leaf.getId())
                    .officerId(officer.getId())
                    .status("PENDING")
                    .assignedAt(assignedTime)
                    .build());
        }
    }
}

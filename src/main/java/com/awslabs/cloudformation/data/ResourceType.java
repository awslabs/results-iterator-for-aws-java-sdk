package com.awslabs.cloudformation.data;

/**
 * A non-exhaustive list of resource types extracted from the AWS SDK for Java v2 (2.16.74-SNAPSHOT)
 */
public enum ResourceType {
    AWS_IAM_Role("AWS::IAM::Role"),
    AWS_KMS_Key("AWS::KMS::Key"),
    AWS_Lambda_Function("AWS::Lambda::Function"),
    AWS_Lambda_LayerVersion("AWS::Lambda::LayerVersion"),
    AWS_S3_Bucket("AWS::S3::Bucket"),
    AWS_SQS_Queue("AWS::SQS::Queue"),
    AWS_SecretsManager_Secret("AWS::SecretsManager::Secret"),
    AWS_ACM_Certificate("AWS::ACM::Certificate"),
    AWS_ApiGateway_RestApi("AWS::ApiGateway::RestApi"),
    AWS_ApiGateway_Stage("AWS::ApiGateway::Stage"),
    AWS_ApiGatewayV2_Api("AWS::ApiGatewayV2::Api"),
    AWS_ApiGatewayV2_Stage("AWS::ApiGatewayV2::Stage"),
    AWS_AutoScaling_AutoScalingGroup("AWS::AutoScaling::AutoScalingGroup"),
    AWS_AutoScaling_LaunchConfiguration("AWS::AutoScaling::LaunchConfiguration"),
    AWS_AutoScaling_ScalingPolicy("AWS::AutoScaling::ScalingPolicy"),
    AWS_AutoScaling_ScheduledAction("AWS::AutoScaling::ScheduledAction"),
    AWS_CloudFormation_Stack("AWS::CloudFormation::Stack"),
    AWS_CloudFront_Distribution("AWS::CloudFront::Distribution"),
    AWS_CloudFront_StreamingDistribution("AWS::CloudFront::StreamingDistribution"),
    AWS_CloudTrail_Trail("AWS::CloudTrail::Trail"),
    AWS_CloudWatch_Alarm("AWS::CloudWatch::Alarm"),
    AWS_CodeBuild_Project("AWS::CodeBuild::Project"),
    AWS_CodePipeline_Pipeline("AWS::CodePipeline::Pipeline"),
    AWS_Config_ConformancePackCompliance("AWS::Config::ConformancePackCompliance"),
    AWS_Config_ResourceCompliance("AWS::Config::ResourceCompliance"),
    AWS_DynamoDB_Table("AWS::DynamoDB::Table"),
    AWS_EC2_CustomerGateway("AWS::EC2::CustomerGateway"),
    AWS_EC2_EIP("AWS::EC2::EIP"),
    AWS_EC2_EgressOnlyInternetGateway("AWS::EC2::EgressOnlyInternetGateway"),
    AWS_EC2_FlowLog("AWS::EC2::FlowLog"),
    AWS_EC2_Host("AWS::EC2::Host"),
    AWS_EC2_Instance("AWS::EC2::Instance"),
    AWS_EC2_InternetGateway("AWS::EC2::InternetGateway"),
    AWS_EC2_NatGateway("AWS::EC2::NatGateway"),
    AWS_EC2_NetworkAcl("AWS::EC2::NetworkAcl"),
    AWS_EC2_NetworkInterface("AWS::EC2::NetworkInterface"),
    AWS_EC2_RegisteredHAInstance("AWS::EC2::RegisteredHAInstance"),
    AWS_EC2_RouteTable("AWS::EC2::RouteTable"),
    AWS_EC2_SecurityGroup("AWS::EC2::SecurityGroup"),
    AWS_EC2_Subnet("AWS::EC2::Subnet"),
    AWS_EC2_VPC("AWS::EC2::VPC"),
    AWS_EC2_VPCEndpoint("AWS::EC2::VPCEndpoint"),
    AWS_EC2_VPCEndpointService("AWS::EC2::VPCEndpointService"),
    AWS_EC2_VPCPeeringConnection("AWS::EC2::VPCPeeringConnection"),
    AWS_EC2_VPNConnection("AWS::EC2::VPNConnection"),
    AWS_EC2_VPNGateway("AWS::EC2::VPNGateway"),
    AWS_EC2_Volume("AWS::EC2::Volume"),
    AWS_ElasticBeanstalk_Application("AWS::ElasticBeanstalk::Application"),
    AWS_ElasticBeanstalk_ApplicationVersion("AWS::ElasticBeanstalk::ApplicationVersion"),
    AWS_ElasticBeanstalk_Environment("AWS::ElasticBeanstalk::Environment"),
    AWS_ElasticLoadBalancing_LoadBalancer("AWS::ElasticLoadBalancing::LoadBalancer"),
    AWS_ElasticLoadBalancingV2_LoadBalancer("AWS::ElasticLoadBalancingV2::LoadBalancer"),
    AWS_Elasticsearch_Domain("AWS::Elasticsearch::Domain"),
    AWS_IAM_Group("AWS::IAM::Group"),
    AWS_IAM_Policy("AWS::IAM::Policy"),
    AWS_IAM_User("AWS::IAM::User"),
    AWS_NetworkFirewall_Firewall("AWS::NetworkFirewall::Firewall"),
    AWS_NetworkFirewall_FirewallPolicy("AWS::NetworkFirewall::FirewallPolicy"),
    AWS_NetworkFirewall_RuleGroup("AWS::NetworkFirewall::RuleGroup"),
    AWS_QLDB_Ledger("AWS::QLDB::Ledger"),
    AWS_RDS_DBCluster("AWS::RDS::DBCluster"),
    AWS_RDS_DBClusterSnapshot("AWS::RDS::DBClusterSnapshot"),
    AWS_RDS_DBInstance("AWS::RDS::DBInstance"),
    AWS_RDS_DBSecurityGroup("AWS::RDS::DBSecurityGroup"),
    AWS_RDS_DBSnapshot("AWS::RDS::DBSnapshot"),
    AWS_RDS_DBSubnetGroup("AWS::RDS::DBSubnetGroup"),
    AWS_RDS_EventSubscription("AWS::RDS::EventSubscription"),
    AWS_Redshift_Cluster("AWS::Redshift::Cluster"),
    AWS_Redshift_ClusterParameterGroup("AWS::Redshift::ClusterParameterGroup"),
    AWS_Redshift_ClusterSecurityGroup("AWS::Redshift::ClusterSecurityGroup"),
    AWS_Redshift_ClusterSnapshot("AWS::Redshift::ClusterSnapshot"),
    AWS_Redshift_ClusterSubnetGroup("AWS::Redshift::ClusterSubnetGroup"),
    AWS_Redshift_EventSubscription("AWS::Redshift::EventSubscription"),
    AWS_S3_AccountPublicAccessBlock("AWS::S3::AccountPublicAccessBlock"),
    AWS_SNS_Topic("AWS::SNS::Topic"),
    AWS_SSM_AssociationCompliance("AWS::SSM::AssociationCompliance"),
    AWS_SSM_FileData("AWS::SSM::FileData"),
    AWS_SSM_ManagedInstanceInventory("AWS::SSM::ManagedInstanceInventory"),
    AWS_SSM_PatchCompliance("AWS::SSM::PatchCompliance"),
    AWS_ServiceCatalog_CloudFormationProduct("AWS::ServiceCatalog::CloudFormationProduct"),
    AWS_ServiceCatalog_CloudFormationProvisionedProduct("AWS::ServiceCatalog::CloudFormationProvisionedProduct"),
    AWS_ServiceCatalog_Portfolio("AWS::ServiceCatalog::Portfolio"),
    AWS_Shield_Protection("AWS::Shield::Protection"),
    AWS_ShieldRegional_Protection("AWS::ShieldRegional::Protection"),
    AWS_WAF_RateBasedRule("AWS::WAF::RateBasedRule"),
    AWS_WAF_Rule("AWS::WAF::Rule"),
    AWS_WAF_RuleGroup("AWS::WAF::RuleGroup"),
    AWS_WAF_WebACL("AWS::WAF::WebACL"),
    AWS_WAFRegional_RateBasedRule("AWS::WAFRegional::RateBasedRule"),
    AWS_WAFRegional_Rule("AWS::WAFRegional::Rule"),
    AWS_WAFRegional_RuleGroup("AWS::WAFRegional::RuleGroup"),
    AWS_WAFRegional_WebACL("AWS::WAFRegional::WebACL"),
    AWS_WAFv2_IPSet("AWS::WAFv2::IPSet"),
    AWS_WAFv2_ManagedRuleSet("AWS::WAFv2::ManagedRuleSet"),
    AWS_WAFv2_RegexPatternSet("AWS::WAFv2::RegexPatternSet"),
    AWS_WAFv2_RuleGroup("AWS::WAFv2::RuleGroup"),
    AWS_WAFv2_WebACL("AWS::WAFv2::WebACL"),
    AWS_XRay_EncryptionConfig("AWS::XRay::EncryptionConfig"),
    AWS_SageMaker_ProcessingJob("AWS::SageMaker::ProcessingJob"),
    AWS_SageMaker_TrainingJob("AWS::SageMaker::TrainingJob"),
    AWS_SageMaker_TransformJob("AWS::SageMaker::TransformJob");

    private final String value;

    ResourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

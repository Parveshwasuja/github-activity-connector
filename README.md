# github-activity-connector
To fetch the repository and commits

Hit this rest endpoint and get the required response of repositories and 20 most recent commits for each repository fetched : http://localhost:8080/api/github/repos/<github_user-name>

Headers : 
Key : Authorization
Value : Personal Access token

Sample request : http://localhost:8080/api/github/repos/Parveshwasuja
Sample Response : 
[
    {
        "name": "Airline_Reservation",
        "fullName": "Parveshwasuja/Airline_Reservation",
        "url": "https://github.com/Parveshwasuja/Airline_Reservation",
        "commits": [
            {
                "message": "Updated this file",
                "author": "Parveshwasuja",
                "timestamp": "2018-12-16T09:59:05Z"
            },
            {
                "message": "Files uploaded",
                "author": "Parveshwasuja",
                "timestamp": "2018-12-16T09:37:44Z"
            }
        ]
    }
]

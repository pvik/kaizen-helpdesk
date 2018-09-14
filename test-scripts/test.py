#!/usr/bin/python

import requests
import pprint


URL = 'http://localhost:3000/'

admin_cred = {'username': "admin", 'password': "khd#admin"}
tech1_cred = {'username': "tech1", 'password': "tech1"}
tech2_cred = {'username': "tech2", 'password': "tech2"}

# Pretty printer
pp = pprint.PrettyPrinter(width=75, compact=True)

def get_jwt(cred):
    try:
        r = requests.get(url = URL+"login", params = cred)
        r.raise_for_status()
        return r.json()['token']
    except requests.exceptions.RequestException as e:
        print(e)
        #exit(1)

def get_ticket(jwt, ticket_id):
    try:
        r = requests.get(url = URL+"api/ticket/"+str(ticket_id),
                         headers={'Authorization': 'Token '+jwt})
        r.raise_for_status()
        return r.json()
    except requests.exceptions.RequestException as e:
        print(e)
        exit(1)

def perform_tests(cred):
    username = cred['username']

    print("Running tests as " + username + "\n")
    
    print(' - Getting JWT as ' + username)
    jwt = get_jwt(cred)

    if jwt:
        print(' - Getting Ticket #1 as ' + username)
        pp.pprint(get_ticket(jwt, 1))

    print("======")

    
def main():

    perform_tests({'username': 'fail', 'password': 'pass'})
    
    perform_tests(admin_cred)
    perform_tests(tech1_cred)

if __name__ == '__main__':
    main()
